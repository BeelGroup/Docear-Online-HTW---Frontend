package controllers;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.MutableDateTime;
import play.Logger;
import play.Play;
import play.mvc.*;
import play.mvc.Http.*;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.joda.time.DateTime.*;

public class Secured extends Security.Authenticator {
    public static final String SESSION_KEY_USERNAME = "username";
    public static final String SESSION_KEY_TIMEOUT = "session-timeout";
    public static final String EPOCHE_START = "0000-01-01T00:00:00.000Z";

    @Override
    public String getUsername(Context ctx) {
        final Http.Session session = ctx.session();
        String username = session.get(SESSION_KEY_USERNAME);
        if (username != null && sessionTimeoutOccurred(ctx)) {
            Logger.debug("timeout for " + session.get(SESSION_KEY_USERNAME));
            session.clear();
            username = null;
        }
        return username;
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        ctx.flash().put("error", "You need to authenticate.");
        return redirect(routes.Application.index());
    }

    public static Instant createTimeoutTimestamp() {
        final int timeoutInSeconds = Play.application().configuration().getInt("session.timeoutInSeconds");
        return new Instant().plus(Duration.standardSeconds(timeoutInSeconds));
    }

    private boolean sessionTimeoutOccurred(Context ctx) {
        final String timeout = defaultString(ctx.session().get(SESSION_KEY_TIMEOUT), EPOCHE_START);
        final MutableDateTime invalidateTime = new Instant(timeout).toMutableDateTime();
        return now().isAfter(invalidateTime);
    }
}