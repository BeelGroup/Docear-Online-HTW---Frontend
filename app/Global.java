import configuration.SpringConfiguration;
import controllers.featuretoggle.Feature;
import controllers.routes;
import info.schleichardt.play2.basicauth.CredentialsFromConfChecker;
import info.schleichardt.play2.basicauth.JAuthenticator;
import models.frontend.LoggedError;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import play.*;
import play.api.libs.Collections;
import play.cache.Cache;
import play.api.mvc.Handler;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.apache.commons.lang.StringUtils.defaultString;
import static play.mvc.Controller.flash;
import static play.mvc.Results.notFound;
import static play.mvc.Results.redirect;
import static controllers.Application.LOGGED_ERROR_CACHE_PREFIX;

public class Global extends GlobalSettings {
    private int loggedErrorExpirationInSeconds;
    private final JAuthenticator authenticator = new JAuthenticator(new CredentialsFromConfChecker());
    private boolean basicAuthEnabled;

    @Override
    public void onStart(Application application) {
        final Configuration conf = Play.application().configuration();
        logConfiguration(conf);
        initializeSpring();
        initializeBasicAuthPlugin();
        loggedErrorExpirationInSeconds = conf.getInt("application.logged.error.expirationInSeconds");
        super.onStart(application);
        initializeFeatureToggles(conf);
        logGit(application);
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
        String possibleFeaturesString = StringUtils.join(Feature.values(), ", ");
        Logger.info("possible features: " + possibleFeaturesString);

        List<String> enabledFeatures = conf.getStringList("application.features");
        if (enabledFeatures == null) {
            enabledFeatures = new LinkedList<String>();
        }
        String enabledFeaturesString = StringUtils.join(enabledFeatures, ", ");
        Logger.info("enabled features: " + enabledFeaturesString);
        for (final String feature : enabledFeatures) {
            Feature.enableFeature(feature, true);
        }
    }

    @Override
    public Handler onRouteRequest(final Http.RequestHeader requestHeader) {
        Handler handler = super.onRouteRequest(requestHeader);
        if(basicAuthEnabled) {
            handler = authenticator.requireBasicAuthentication(requestHeader, handler);
        }
        return handler;
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
        Here is no HTTP context available to use the standard templates.
        So the error gets an ID, stored with that ID in the cache and the redirected action has a
        HTTP context and can restore the exceptions from the cache and use the standard templates.
         */
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
        if(Logger.isDebugEnabled() && !request.path().startsWith("/assets")) {
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

    private void initializeBasicAuthPlugin() {
        basicAuthEnabled = isTrue(Play.application().configuration().getBoolean("basic.auth.enabled"));//use -Dbasic.auth.enabled=true
        Logger.info(basicAuthEnabled ? "basic auth is enabled" : "basic auth is disabled");
    }

    @Override
    public Result onHandlerNotFound(Http.RequestHeader requestHeader) {
        Logger.warn("404 for " + requestHeader);
        if (requestHeader.accepts("text/html")) {
            flash("warning", "Page not found");
            return notFound(views.html.status404.render(requestHeader));
        } else {
            ObjectNode result = Json.newObject();
            result.put("message", "not found");
            return notFound(result);
        }
    }
}
