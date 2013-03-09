package controllers;

import static play.data.Form.form;
import info.schleichardt.play2.mailplugin.Mailer;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.frontend.FeedbackFormData;
import models.frontend.LoggedError;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import play.Play;
import play.Routes;
import play.cache.Cache;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;

public class Application extends Controller {
    public static final String LOGGED_ERROR_CACHE_PREFIX = "logged.error.";
    public static final Form<FeedbackFormData> feedbackForm = form(FeedbackFormData.class);
    

	/** displays current mind map drawing 
	 * @throws EmailException */
	public static Result index() {
		if(User.isAuthenticated()) {
			return ok(views.html.home.render());
		} else {
			return ok(views.html.index.render(User.credentialsForm));
		}
	}

    /** displays a feature list and help site */
	public static Result help() {
		return ok(views.html.help.render());
	}
	
	public static Result feedback() throws EmailException {
		Form<FeedbackFormData> filledForm = feedbackForm.bindFromRequest();
		
		if(filledForm.hasErrors()) {
			return badRequest();
		} else {
			final FeedbackFormData data = filledForm.get();
			
			//data from request and config
			final String subject = (data.getFeedbackSubject() != null ? data.getFeedbackSubject() : "New Feedback");
			final String contactLine = "Contact: " + (data.getFeedbackEmail() != null ? data.getFeedbackEmail() : "non provided");
			final String[] sendToAddresses = Play.application().configuration().getString("feedback.sendTo").split(",");
			
			final StringBuilder contentBuilder = new StringBuilder();
			contentBuilder.append(contactLine).append("\n");
			contentBuilder.append("Message:\n").append(data.getFeedbackText());
			contentBuilder.append("\n\nRequest headers:\n");
			for(Entry<String,String[]> entry: request().headers().entrySet()) {
				contentBuilder.append(entry.getKey()).append(" => ");
				for(String value : entry.getValue()) {
					contentBuilder.append(value).append(",");
				}
				contentBuilder.deleteCharAt(contentBuilder.length()-1).append("\n");
			}
			
			final SimpleEmail mail = new SimpleEmail();
			mail.setSubject(subject);
			mail.setFrom("feedback@docear.org");
			//set recipients
			for(String address : sendToAddresses) {
				mail.addTo(address);
			}
			
			mail.setContent(contentBuilder.toString(),"text/plain");
			Mailer.send(mail);
			return ok();
		}
	}

    /** makes some play routes in JavaScript avaiable */
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");



        return ok(
             /* this currently looks like errors in IntelliJ IDEA */
            Routes.javascriptRouter("jsRoutes",
                    routes.javascript.MindMap.map(),
                    routes.javascript.MindMap.mapListFromDB(),
                    routes.javascript.MindMap.changeNode(),
                    routes.javascript.MindMap.createNode(),
                    routes.javascript.MindMap.deleteNode(),
                    routes.javascript.MindMap.addNode()
            )
        );
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
}