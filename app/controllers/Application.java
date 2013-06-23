package controllers;

import static play.data.Form.form;
import info.schleichardt.play2.mailplugin.Mailer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.internet.InternetAddress;

import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.frontend.FeedbackFormData;
import models.frontend.LoggedError;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.Routes;
import play.cache.Cache;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import services.backend.user.UserService;

import com.google.common.collect.Maps;

@Component
public class Application extends DocearController {
    public static final String LOGGED_ERROR_CACHE_PREFIX = "logged.error.";
    public static final Form<FeedbackFormData> feedbackForm = form(FeedbackFormData.class);
    

    @Autowired
    private UserService  userService;
    
	/** 
	 * displays current mind map drawing 
	 */
	public Result index() {
		if(userService.isAuthenticated()) {
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
			Logger.debug("Feedback errors: " + filledForm.errorsAsJson().toString());
			return badRequest(filledForm.errorsAsJson());
		} else {
			final FeedbackFormData data = filledForm.get();
			
			//data from request and config
			final String subject = (data.getFeedbackSubject() != null ? data.getFeedbackSubject() : "New Feedback");
			final String contactLine = "Contact: " + (data.getFeedbackEmail() != null ? data.getFeedbackEmail() : "non provided");
			final String[] sendToAddresses = Play.application().configuration().getString("feedback.sendTo").split(",");
			
			final StringBuilder contentBuilder = new StringBuilder();
			//from whom
			contentBuilder.append(contactLine).append("\n\n");
			//the message
			contentBuilder.append("Message:\n").append(data.getFeedbackText());
			//debug infos
			contentBuilder.append("\n\n\n==================\nDebug infos:\n");
			for(Entry<String,String> entry : debugInfo().entrySet()) {
				contentBuilder.append(entry.getKey())
				.append(" => ")
				.append(entry.getValue())
				.append("\n");
			}
			//request headers from user
			contentBuilder.append("\n\n\n==================\nRequest headers:\n");
			for(Entry<String,String[]> entry: request().headers().entrySet()) {
				contentBuilder.append(entry.getKey()).append(" => ");
				for(String value : entry.getValue()) {
					contentBuilder.append(value).append(",");
				}
				contentBuilder.deleteCharAt(contentBuilder.length()-1).append("\n");
			}
			
			final SimpleEmail mail = new SimpleEmail();
			mail.setSubject(subject);
			mail.setFrom("fb.my.docear@web.de");
			//set recipients
			for(String address : sendToAddresses) {
				mail.addTo(address);
			}
			for(InternetAddress ia : mail.getToAddresses()) {
				Logger.debug("feedback => to address: "+ia.toString());
			}
			mail.setContent(contentBuilder.toString(),"text/plain");
			
			Mailer.send(mail);
			return ok();
		}
	}

    /** makes some play routes in JavaScript available */
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");



        return ok(
             /* this currently looks like errors in IntelliJ IDEA */
            Routes.javascriptRouter("jsRoutes",
                    routes.javascript.User.mapListFromDB(),
                    routes.javascript.User.projectListFromDB(),
                    routes.javascript.MindMap.mapAsJson(),
                    routes.javascript.MindMap.getNode(),
                    routes.javascript.MindMap.createNode(),
                    routes.javascript.MindMap.changeNode(),
                    routes.javascript.MindMap.changeEdge(),
                    routes.javascript.MindMap.moveNode(),
                    routes.javascript.MindMap.deleteNode(),
                    routes.javascript.MindMap.requestLock(),
                    routes.javascript.MindMap.releaseLock(),
                    routes.javascript.MindMap.listenForUpdates(),
                    routes.javascript.MindMap.fetchUpdatesSinceRevision(),
                    routes.javascript.ProjectController.createProject(),
                    routes.javascript.ProjectController.addUserToProject(),
                    routes.javascript.ProjectController.createFolder(),
                    routes.javascript.ProjectController.getFile(),
                    routes.javascript.ProjectController.getProject(),
                    routes.javascript.ProjectController.deleteFile(),
                    routes.javascript.ProjectController.metadata(),
                    routes.javascript.ProjectController.projectVersionDelta(),
                    routes.javascript.ProjectController.putFile(),
                    routes.javascript.ProjectController.moveFile(),
                    routes.javascript.ProjectController.listenForUpdates(),
                    routes.javascript.ProjectController.removeUserFromProject(),
                    routes.javascript.Application.index(),
                    routes.javascript.Assets.at()
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

	public static Result untrail(String path) {
		return movedPermanently("/"+path);
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

    private static String gitCommit;
    public static Map<String, String> debugInfo() {
        Map<String, String> debugInfo = Maps.newTreeMap();
        debugInfo.put("time", new Date().toString());
        debugInfo.put("method", request().method());
        debugInfo.put("uri", request().uri());
        debugInfo.put("host", request().host());
        debugInfo.put("remoteAddress", request().remoteAddress());
        debugInfo.put("User-Agent", request().getHeader("User-Agent"));
        if (gitCommit == null) {
            Properties props = new Properties();
            InputStream inputStream = Play.application().classloader().getResourceAsStream("buildInfo.properties");
            if (inputStream != null) {
                try {
                    props.load(inputStream);
                    gitCommit = props.getProperty("git.newId");
                } catch (IOException e) {
                    Logger.warn("can't find git properties", e);
                }
            }

        }
        debugInfo.put("revision", gitCommit);
        return debugInfo;
    }
}