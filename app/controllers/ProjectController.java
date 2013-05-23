package controllers;

import java.io.IOException;
import java.io.InputStream;

import models.project.formdatas.CreateFolderData;
import models.project.formdatas.CreateProjectData;
import models.project.formdatas.ProjectDeltaData;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.data.Form;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.project.ProjectService;
import services.backend.user.UserService;
import controllers.featuretoggle.Feature;
import controllers.featuretoggle.ImplementedFeature;

@Component
@ImplementedFeature(Feature.WORKSPACE)
public class ProjectController extends Controller {
	final Form<CreateProjectData> createProjectForm = Form.form(CreateProjectData.class);
	final Form<CreateFolderData> createFolderForm = Form.form(CreateFolderData.class);
	final Form<ProjectDeltaData> projectDeltaForm = Form.form(ProjectDeltaData.class);

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;
	
	public Result createProject() throws IOException {
		Form<CreateProjectData> filledForm = createProjectForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final CreateProjectData data = filledForm.get();
			return async(projectService.createProject(username(), data.getName()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	public Result getFile(String projectId, String path) throws IOException {
		return async(projectService.getFile(username(), projectId, path).map(new Function<InputStream, Result>() {

			@Override
			public Result apply(InputStream fileStream) throws Throwable {
				return ok(fileStream);
			}
		}));
	}

	/**
	 * body contains zipped file
	 * @param projectId
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public Result putFile(String projectId, String path) throws IOException {
		final byte[] content = request().body().asRaw().asBytes();
		return async(projectService.putFile(username(), projectId, path, content).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode fileMeta) throws Throwable {
				return ok(fileMeta);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result createFolder() throws IOException {
		Form<CreateFolderData> filledForm = createFolderForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final CreateFolderData data = filledForm.get();
			return async(projectService.createFolder(username(), data.getProjectId(), data.getPath()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result metadata(String projectId, String path) throws IOException {
		return async(projectService.metadata(username(), projectId, path).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode entry) throws Throwable {
				return ok(entry);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result projectVersionDelta() throws IOException {
		Form<ProjectDeltaData> filledForm = projectDeltaForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final ProjectDeltaData data = filledForm.get();
			return async(projectService.versionDelta(username(), data.getProjectId(), data.getCursor()).map(new Function<JsonNode, Result>() {

				@Override
				public Result apply(JsonNode updates) throws Throwable {
					return ok(updates);
				}
			}));
		}

	}

	/**
	 * 
	 * @return name of currently logged in user
	 */
	private String username() {
		final models.backend.User user = userService.getCurrentUser();
		if (user != null)
			return user.getUsername();
		else
			return null;
	}
}
