package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import models.backend.exceptions.sendResult.UnauthorizedException;
import models.project.formdatas.AddUserToProjectData;
import models.project.formdatas.CreateFolderData;
import models.project.formdatas.CreateProjectData;
import models.project.formdatas.DeleteFileData;
import models.project.formdatas.ProjectDeltaData;
import models.project.formdatas.RemoveUserFromProjectData;

import org.apache.commons.lang.Validate;
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
@Security.Authenticated(Secured.class)
public class ProjectController extends Controller {
	final Form<CreateProjectData> createProjectForm = Form.form(CreateProjectData.class);
	final Form<AddUserToProjectData> addUserToProjectForm = Form.form(AddUserToProjectData.class);
	final Form<RemoveUserFromProjectData> removeUserFromProjectForm = Form.form(RemoveUserFromProjectData.class);

	final Form<DeleteFileData> deleteFileForm = Form.form(DeleteFileData.class);
	final Form<CreateFolderData> createFolderForm = Form.form(CreateFolderData.class);
	final Form<ProjectDeltaData> projectDeltaForm = Form.form(ProjectDeltaData.class);

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;

	public Result getProject(String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		return async(projectService.getProjectById(projectId).map(new Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode folderMetadata) throws Throwable {
				return ok(folderMetadata);
			}
		}));
	}

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

	public Result addUserToProject(final String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		Form<AddUserToProjectData> filledForm = addUserToProjectForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final AddUserToProjectData data = filledForm.get();
			return async(projectService.addUserToProject(projectId, data.getUsername()).map(new Function<Boolean, Result>() {
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

	public Result removeUserFromProject(final String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		Form<RemoveUserFromProjectData> filledForm = removeUserFromProjectForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final RemoveUserFromProjectData data = filledForm.get();
			return async(projectService.removeUserFromProject(projectId, data.getUsername()).map(new Function<Boolean, Result>() {
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

	public Result getFile(String projectId, String path) throws IOException {
		assureUserBelongsToProject(projectId);
		return async(projectService.getFile(projectId, path).map(new Function<InputStream, Result>() {

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
	public Result putFile(String projectId, String path, boolean isZip) throws IOException {
		assureUserBelongsToProject(projectId);
		final byte[] content = request().body().asRaw().asBytes();

		/**
		 * To verify if file really is a zip we can check for the signature, a
		 * zip file starts with: 0x504b0304
		 */
		final ByteBuffer byteBuffer = ByteBuffer.wrap(content);
		final int zipSignature = byteBuffer.getInt();
		final boolean isZipValidation = (zipSignature == 0x504b0304);

		if (isZip && !isZipValidation) {
			return badRequest("File was send as zip but isn't.");
		}

		return async(projectService.putFile(projectId, path, content, isZip).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode fileMeta) throws Throwable {
				return ok(fileMeta);
			}
		}));
	}

	public Result deleteFile(String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		Form<DeleteFileData> filledForm = deleteFileForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final DeleteFileData data = filledForm.get();
			return async(projectService.delete(projectId, data.getPath()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	public Result createFolder(String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		Form<CreateFolderData> filledForm = createFolderForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final CreateFolderData data = filledForm.get();
			return async(projectService.createFolder(projectId, data.getPath()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	public Result metadata(String projectId, String path) throws IOException {
		assureUserBelongsToProject(projectId);
		return async(projectService.metadata(projectId, path).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode entry) throws Throwable {
				return ok(entry);
			}
		}));
	}
	
	public Result listenForUpdates() throws IOException {
		final Map<String, Long> projectRevisonMap = new HashMap<String, Long>();
		final Map<String,String[]> urlEncodedBody = request().body().asFormUrlEncoded();
		
		for(Map.Entry<String, String[]> entry : urlEncodedBody.entrySet()) {
			try {
				projectRevisonMap.put(entry.getKey(), Long.parseLong(entry.getValue()[0]));
			} catch(NumberFormatException e) {
				return badRequest("Revisions must be long value!");
			}
		}
		
		//check that project has been send
		if(projectRevisonMap.size() == 0) {
			return badRequest("specify at least one project!");
		}
		
		return async(projectService.listenIfUpdateOccurs(projectRevisonMap).map(new Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode result) throws Throwable {
				return ok(result);
			}
		}));
	}

	public Result projectVersionDelta(String projectId) throws IOException {
		assureUserBelongsToProject(projectId);
		Form<ProjectDeltaData> filledForm = projectDeltaForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final ProjectDeltaData data = filledForm.get();
			return async(projectService.versionDelta(projectId, data.getCursor()).map(new Function<JsonNode, Result>() {

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

	private void assureUserBelongsToProject(String projectId) throws IOException {
		if (!projectService.userBelongsToProject(userService.getCurrentUser().getUsername(), projectId)) {
			throw new UnauthorizedException("User has no rights on Project");
		}
	}
}
