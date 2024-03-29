import configuration.SpringConfiguration;
import controllers.featuretoggle.Feature;
import controllers.featuretoggle.FeatureComparator;
import controllers.routes;
import models.backend.MessageToFrontend;
import models.backend.MessageToFrontend.Type;
import models.backend.exceptions.UserNotFoundException;
import models.backend.exceptions.sendResult.SendResultException;
import models.frontend.LoggedError;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import play.*;
import play.cache.Cache;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

import static controllers.Application.LOGGED_ERROR_CACHE_PREFIX;
import static java.util.Collections.sort;
import static org.apache.commons.lang.StringUtils.defaultString;
import static play.mvc.Controller.flash;
import static play.mvc.Results.notFound;
import static play.mvc.Results.redirect;

public class Global extends GlobalSettings {
    private int loggedErrorExpirationInSeconds;
    private Application application;

    @Override
    public void onStart(Application application) {
        this.application = application;
        final Configuration conf = Play.application().configuration();
        logConfiguration(conf);
        initializeSpring();
        loggedErrorExpirationInSeconds = conf.getInt("application.logged.error.expirationInSeconds");
        super.onStart(application);
        initializeFeatureToggles(conf);
        logGit(application);
    }

    @Override
    public void onStop(Application application) {
        super.onStop(application);
        Logger.info("shutting play application down");
    }

    private void logGit(Application application) {
        try {
            Properties propertiesFromClasspath = getPropertiesFromClasspath("buildInfo.properties", application);
            Set<Map.Entry<Object, Object>> entries = propertiesFromClasspath.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Logger.info(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            Logger.error("can't load git info", e);
        }
    }

    private Properties getPropertiesFromClasspath(String propFileName, Application application) throws IOException {
        Properties props = new Properties();
        InputStream inputStream = application.classloader().getResourceAsStream(propFileName);
        if (inputStream == null) {
            throw new FileNotFoundException("property file '" + propFileName
                    + "' not found in the classpath");
        }
        props.load(inputStream);
        return props;
    }

    private void initializeFeatureToggles(Configuration conf) {
        final List<Feature> possibleFeatures = Arrays.asList(Feature.values());
        sort(possibleFeatures, new FeatureComparator());
        String possibleFeaturesString = StringUtils.join(possibleFeatures, ", ");
        Logger.info("possible features: " + possibleFeaturesString);
        final Configuration featureConf = conf.getConfig("application.features");
        List<String> enabledFeatureList = new LinkedList<String>();
        for (String feature : featureConf.keys()) {
            if (featureConf.getBoolean(feature)) {
                Feature.enableFeature(feature, true);
                enabledFeatureList.add(feature);
            }

        }
        sort(enabledFeatureList);
        String enabledFeaturesString = StringUtils.join(enabledFeatureList, ", ");
        Logger.info("enabled features: " + enabledFeaturesString);
    }

    @Override
    public Action onRequest(Http.Request request, Method method) {
        logRequest(request, method);
        return super.onRequest(request, method);
    }

    @Override
    public Result onError(Http.RequestHeader requestHeader, Throwable throwable) {
        Logger.error("can't answer request properly", throwable);
        /*
        For Play 2.0.4:
        Here is no HTTP context available to use the standard templates.
        So the error gets an ID, stored with that ID in the cache and the redirected action has a
        HTTP context and can restore the exceptions from the cache and use the standard templates.
         */

        //check if exception needs special handling
        final Throwable realThrowable = throwable.getCause();
        if (realThrowable instanceof SendResultException) {
            final SendResultException e = (SendResultException) realThrowable;
            final MessageToFrontend message = new MessageToFrontend(Type.error, e.getMessage());
            final JsonNode jsonNode = message.toJsonNode();

            return Controller.status(e.getStatusCode(), jsonNode);
        } else if (realThrowable instanceof UserNotFoundException) {
            return Controller.unauthorized();
        }
        //default handling
        final String errorId = UUID.randomUUID().toString();
        Cache.set(LOGGED_ERROR_CACHE_PREFIX + errorId, new LoggedError(requestHeader, throwable), loggedErrorExpirationInSeconds);
        if (Play.application().isDev()) {
            return super.onError(requestHeader, throwable);//still show errors in browser
        } else {
            return redirect(routes.Application.error(errorId));
        }
    }

    @Override
    public <A> A getControllerInstance(Class<A> clazz) throws Exception {
        A bean = SpringConfiguration.getBean(clazz);
        if (bean == null) {
            bean = super.getControllerInstance(clazz);
        }
        return bean;
    }

    private void logConfiguration(Configuration conf) {
        final String configFilename = defaultString(conf.getString("config.file"), "conf/application.conf");
        Logger.info("using configuration " + configFilename);
    }

    private void logRequest(Http.Request request, Method method) {
        if (Logger.isDebugEnabled() && !request.path().startsWith("/assets")) {
            StringBuilder sb = new StringBuilder(request.toString());
            sb.append(" ").append(method.getDeclaringClass().getCanonicalName());
            sb.append(".").append(method.getName()).append("(");
            Class<?>[] params = method.getParameterTypes();
            for (int j = 0; j < params.length; j++) {
                sb.append(params[j].getCanonicalName().replace("java.lang.", ""));
                if (j < (params.length - 1))
                    sb.append(',');
            }
            sb.append(")");
            Logger.debug(sb.toString());
        }
    }

    private void initializeSpring() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        configuration.SpringConfiguration.initializeContext(context);
    }

    @Override
    public Result onHandlerNotFound(Http.RequestHeader requestHeader) {
        Logger.warn("404 for " + requestHeader);
        if (Play.isDev() && application.configuration().getBoolean("application.showdev404page")) {
            return null;
        } else if (requestHeader.accepts("text/html")) {
            flash("warning", "Page not found");
            return notFound(views.html.status404.render(requestHeader));
        } else {
            ObjectNode result = Json.newObject();
            result.put("message", "not found");
            return notFound(result);
        }
    }
}
