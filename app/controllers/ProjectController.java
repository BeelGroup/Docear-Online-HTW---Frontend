package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import models.backend.exceptions.sendResult.UnauthorizedException;
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
@Security.Authenticated(Secured.class)
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

	public Result getProject(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        return async(projectService.getProjectById(username(), projectId).map(new Function<JsonNode, Result>() {
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
			return async(projectService.addUserToProject(username(), projectId, data.getUsername()).map(new Function<Boolean, Result>() {
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
			return async(projectService.removeUserFromProject(username(), projectId, data.getUsername()).map(new Function<Boolean, Result>() {
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
	public Result putFile(String projectId, String path, boolean isZip) throws IOException {
        assureUserBelongsToProject(projectId);
		final byte[] content = request().body().asRaw().asBytes();
		
		/**
		 * To verify if file really is a zip
		 * we can check for the signature, a zip file starts with: 0x504b0304
		 */
		final ByteBuffer byteBuffer = ByteBuffer.wrap(content);
		final int zipSignature = byteBuffer.getInt();
		final boolean isZipValidation = (zipSignature == 0x504b0304);
		
		if(isZip && !isZipValidation) {
			return badRequest("File was send as zip but isn't.");
		}
		
		return async(projectService.putFile(username(), projectId, path, content, isZip).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode fileMeta) throws Throwable {
				return ok(fileMeta);
			}
		}));
	}

	public Result createFolder(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
		Form<CreateFolderData> filledForm = createFolderForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final CreateFolderData data = filledForm.get();
			return async(projectService.createFolder(username(), projectId, data.getPath()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	public Result metadata(String projectId, String path) throws IOException {
        assureUserBelongsToProject(projectId);
		return async(projectService.metadata(username(), projectId, path).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode entry) throws Throwable {
				return ok(entry);
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
			return async(projectService.versionDelta(username(), projectId, data.getCursor()).map(new Function<JsonNode, Result>() {

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
