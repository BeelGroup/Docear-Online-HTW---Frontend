import configuration.SpringConfiguration;
import controllers.routes;
import info.schleichardt.play2.basicauth.CredentialsFromConfChecker;
import info.schleichardt.play2.basicauth.JAuthenticator;
import models.frontend.LoggedError;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import play.*;
import play.cache.Cache;
import play.api.mvc.Handler;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.apache.commons.lang.StringUtils.defaultString;
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
        return redirect(routes.Application.error(errorId));
    }

    @Override
    public <A> A getControllerInstance(Class<A> clazz) {
        return SpringConfiguration.getBean(clazz);
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
}
