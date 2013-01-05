package controllers;

import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.frontend.LoggedError;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;

public class Application extends Controller {
    public static final String LOGGED_ERROR_CACHE_PREFIX = "logged.error.";

	/** displays current mind map drawing */
	public static Result index() {
		return ok(views.html.index.render());
	}

    /** displays a feature list and help site */
	public static Result help() {
		return ok(views.html.help.render());
	}

	/** for evolving mvc structure on client side */
	public static Result mvc() {
		return ok(views.html.mvc.render());
	}

	/** global error page for 500 Internal Server Error */
	public static Result error(String errorId) {
        final LoggedError loggedError = (LoggedError) Cache.get(LOGGED_ERROR_CACHE_PREFIX + errorId);
        boolean isJson = false;

        String message = "An error has occurred.";
        if (loggedError != null) {
            isJson = isRequestForJson(loggedError);
            try {
                Throwable t = loggedError.getThrowable();
                while (t.getCause() != null) {
                    t = t.getCause();
                }
                throw t;
            } catch (NoUserLoggedInException e) {
                message = "You need to be logged in to perform this action.";
            } catch (DocearServiceException e) {
                message = "An error has occurred.";
            } catch (IOException e) {
                message = "Can't connect to backend.";
            } catch (Throwable throwable) {
                message = "System error.";
            }
        }

        Result result;
        if (isJson) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jNode = mapper.createObjectNode();
            jNode.put("message", message);
            result =  internalServerError(jNode);
        } else {
            flash("error", message);
            result =  internalServerError(views.html.error.render());
        }
        return result;
    }

    private static boolean isRequestForJson(LoggedError loggedError) {
        boolean isJson = false;
        boolean found = false;
        final List<String> list = loggedError.getRequestHeader().accept();
        for (int i = 0; i < list.size() && !found; i++) {
            final String element = list.get(i);
            if ("text/html".equals(element)) {
                isJson = false;
                found = true;
            } else if("application/json".equals(element)) {
                isJson = true;
                found = true;
            }
        }
        return isJson;
    }

    public static Result smallSolutions() {
		return ok(views.html.smallSolutions.render("Solutions"));
	}

	public static String getSessionCookieName() {
		return Play.application().configuration().getString("backend.sessionIdName");
	}
}