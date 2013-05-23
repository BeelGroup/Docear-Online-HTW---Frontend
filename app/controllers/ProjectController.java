package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import models.project.formdatas.AddUserToProjectData;
import models.project.formdatas.CreateFolderData;
import models.project.formdatas.CreateProjectData;
import models.project.formdatas.ProjectDeltaData;
import models.project.formdatas.RemoveUserFromProjectData;

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
	final Form<AddUserToProjectData> addUserToProjectForm = Form.form(AddUserToProjectData.class);
	final Form<RemoveUserFromProjectData> removeUserFromProjectForm = Form.form(RemoveUserFromProjectData.class);

	final Form<CreateFolderData> createFolderForm = Form.form(CreateFolderData.class);
	final Form<ProjectDeltaData> projectDeltaForm = Form.form(ProjectDeltaData.class);

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;

	@Security.Authenticated(Secured.class)
	public Result getProject(String projectId) throws IOException {
		return async(projectService.getProjectById(username(), projectId).map(new Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode folderMetadata) throws Throwable {
				return ok(folderMetadata);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
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

	@Security.Authenticated(Secured.class)
	public Result addUserToProject() throws IOException {
		Form<AddUserToProjectData> filledForm = addUserToProjectForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final AddUserToProjectData data = filledForm.get();
			return async(projectService.addUserToProject(username(), data.getProjectId(), data.getUsername()).map(new Function<Boolean, Result>() {
				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success)
						return ok();
					else {
						return internalServerError("Unknown Error occured");
					}
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result removeUserFromProject() throws IOException {
		Form<RemoveUserFromProjectData> filledForm = removeUserFromProjectForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final RemoveUserFromProjectData data = filledForm.get();
			return async(projectService.removeUserFromProject(username(), data.getProjectId(), data.getUsername()).map(new Function<Boolean, Result>() {
				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success)
						return ok();
					else {
						return internalServerError("Unknown Error occured");
					}
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
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
	 * 
	 * @param projectId
	 * @param path
	 * @return
	 * @throws IOException
	 */
	@Security.Authenticated(Secured.class)
	public Result putFile(String projectId, String path) throws IOException {
		final byte[] content = request().body().asRaw().asBytes();
		
		/**
		 * To recognize if the uploaded file is a zip 
		 * we can check for the signature, a zip file starts with: 0x504b0304
		 */
		final ByteBuffer byteBuffer = ByteBuffer.wrap(content);
		final int zipSignature = byteBuffer.getInt();
		final boolean isZip = (zipSignature == 0x504b0304);
		
		return async(projectService.putFile(username(), projectId, path, content, isZip).map(new Function<JsonNode, Result>() {

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
