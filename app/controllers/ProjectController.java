package controllers;

import controllers.featuretoggle.Feature;
import controllers.featuretoggle.ImplementedFeature;
import controllers.secured.SecuredRest;
import models.backend.exceptions.sendResult.SendResultException;
import models.backend.exceptions.sendResult.UnauthorizedException;
import models.project.formdatas.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.Logger;
import play.data.Form;
import play.libs.F.Function;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.project.ProjectService;
import services.backend.project.VersionDeltaResponse;
import services.backend.project.persistance.EntityCursor;
import services.backend.project.persistance.FileMetaData;
import services.backend.project.persistance.Project;
import services.backend.user.UserService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@ImplementedFeature(Feature.WORKSPACE)
@Security.Authenticated(SecuredRest.class)
public class ProjectController extends DocearController {
    final Form<CreateProjectData> createProjectForm = Form.form(CreateProjectData.class);
    final Form<AddUserToProjectData> addUserToProjectForm = Form.form(AddUserToProjectData.class);
    final Form<RemoveUserFromProjectData> removeUserFromProjectForm = Form.form(RemoveUserFromProjectData.class);
    final Form<DeleteFileData> deleteFileForm = Form.form(DeleteFileData.class);
    final Form<MoveData> moveForm = Form.form(MoveData.class);
    final Form<CreateFolderData> createFolderForm = Form.form(CreateFolderData.class);
    final Form<ProjectDeltaData> projectDeltaForm = Form.form(ProjectDeltaData.class);
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;

    public Result getProject(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        final Project project = projectService.getProjectById(projectId);
        return ok(new ObjectMapper().valueToTree(project));
    }

    public Result createProject() throws IOException {
        Form<CreateProjectData> filledForm = createProjectForm.bindFromRequest();
        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final CreateProjectData data = filledForm.get();
            final Project project = projectService.createProject(username(), data.getName());
            return ok(new ObjectMapper().valueToTree(project));
        }
    }

    public Result addUserToProject(final String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<AddUserToProjectData> filledForm = addUserToProjectForm.bindFromRequest();

        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final AddUserToProjectData data = filledForm.get();
            projectService.addUserToProject(projectId, data.getUsername());
            return ok();
        }
    }

    public Result removeUserFromProject(final String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<RemoveUserFromProjectData> filledForm = removeUserFromProjectForm.bindFromRequest();

        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final RemoveUserFromProjectData data = filledForm.get();
            final boolean keepLastUser = true;
            final boolean removed = projectService.removeUserFromProject(projectId, data.getUsername(), keepLastUser);
            return removed ? ok() : status(PRECONDITION_FAILED);
        }
    }

    public Result getFile(String projectId, String path, boolean zipped) throws IOException {
        assureUserBelongsToProject(projectId);
        path = normalizePath(path);
        return ok(projectService.getFile(projectId, path, zipped));
    }

    /**
     * body contains zipped file
     *
     * @param projectId
     * @param path
     * @return
     * @throws IOException
     */
    @BodyParser.Of(BodyParser.Raw.class)
    public Result putFile(String projectId, String path, boolean isZip, Long parentRev, Long contentLength) throws IOException {
        assureUserBelongsToProject(projectId);
        path = normalizePath(path);
        byte[] content = request().body().asRaw().asBytes();


        //can't use null in router, so -1 is given and will be mapped to null
        if (parentRev == -1)
            parentRev = null;

        //check that content has correct length
        if(contentLength > 0 && contentLength.intValue() != content.length) {
            throw new SendResultException("File was not fully uploaded!",400);
        }

        boolean isZipValidation = false;
        //check that content is present
        if (content == null) {
            content = new byte[0];
        } else {

            /**
             * To verify if file really is a zip we can check for the signature, a
             * zip file starts with: 0x504b0304
             */
            try {
                final ByteBuffer byteBuffer = ByteBuffer.wrap(content);
                final int zipSignature = byteBuffer.getInt();
                isZipValidation = (zipSignature == 0x504b0304);
                Logger.debug("putFile => send as zip: "+isZip+"; validated: "+isZipValidation);
            } catch (BufferUnderflowException e) {
                isZipValidation = false;
                // do nothing
            }
        }

        Logger.debug("byte count: " + content.length);

        if (isZip && !isZipValidation) {
            return badRequest("File was send as zip but isn't.");
        }
        return ok(projectService.putFile(projectId, path, content, isZip, parentRev, false));
    }

    public Result moveFile(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<MoveData> filledForm = moveForm.bindFromRequest();
        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final MoveData data = filledForm.get();
            projectService.moveFile(projectId, data.getCurrentPath(), data.getMoveToPath());
            return ok(new ObjectMapper().readTree("[\"success\"]"));
        }
    }

    public Result deleteFile(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<DeleteFileData> filledForm = deleteFileForm.bindFromRequest();

        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final DeleteFileData data = filledForm.get();
            return ok(projectService.delete(projectId, data.getPath()));
        }
    }

    public Result createFolder(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<CreateFolderData> filledForm = createFolderForm.bindFromRequest();

        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final CreateFolderData data = filledForm.get();
            final FileMetaData newMetaData = projectService.createFolder(projectId, data.getPath());
            final JsonNode jsonNode = new ObjectMapper().valueToTree(newMetaData);
            return ok(jsonNode);
        }
    }

    public Result metadata(String projectId, String path) throws IOException {
        assureUserBelongsToProject(projectId);
        path = normalizePath(path);
        final FileMetaData metadata = projectService.metadata(projectId, path);
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode metadataJson = mapper.valueToTree(metadata);

        // get children for dir
        if (metadata.isDir()) {
            Logger.debug("is Folder");
            final List<FileMetaData> childrenData = new ArrayList<FileMetaData>();
            final EntityCursor<FileMetaData> childrenMetadatas = projectService.getMetaDataOfDirectChildren(projectId, path, 5000);

            try {
                for (FileMetaData childMetadata : childrenMetadatas) {
                    Logger.debug(childMetadata.toString());
                    if (!childMetadata.isDeleted())
                        childrenData.add(childMetadata);
                }
                final JsonNode contentsJson = mapper.valueToTree(childrenData);
                metadataJson.put("contents", contentsJson);
            } finally {
                childrenMetadatas.close();
            }

        }
        return ok(metadataJson);
    }

    public Result listenForUpdates(boolean longPolling) throws IOException {
        final Map<String, Long> projectRevisonMap = new HashMap<String, Long>();
        final Map<String, String[]> urlEncodedBody = request().body().asFormUrlEncoded();

        for (Map.Entry<String, String[]> entry : urlEncodedBody.entrySet()) {
            final String projectId = entry.getKey();
            try {
                projectRevisonMap.put(projectId, Long.parseLong(entry.getValue()[0]));
            } catch (NumberFormatException e) {
                return badRequest("Revisions must be long value!");
            }
        }

        return async(projectService.listenIfUpdateOccurs(username(), projectRevisonMap,longPolling).map(new Function<JsonNode, Result>() {
            @Override
            public Result apply(JsonNode result) throws Throwable {
                return ok(result);
            }
        }).recover(new Function<Throwable, Result>() {
            @Override
            public Result apply(Throwable t) throws Throwable {
                if (t instanceof TimeoutException) {
                    return status(Controller.NOT_MODIFIED);
                } else
                    throw t;
            }
        }));
    }

    public Result projectVersionDelta(String projectId) throws IOException {
        assureUserBelongsToProject(projectId);
        Form<ProjectDeltaData> filledForm = projectDeltaForm.bindFromRequest();

        if (projectId.equals("-1"))
            return status(NOT_MODIFIED);

        if (filledForm.hasErrors()) {
            return badRequest(filledForm.errorsAsJson());
        } else {
            final ProjectDeltaData data = filledForm.get();
            final VersionDeltaResponse response = projectService.versionDelta(projectId, data.getProjectRevision());
            return ok(new ObjectMapper().valueToTree(response));
        }

    }

    /**
     * @return name of currently logged in user
     */
    private String username() {
        final models.backend.User user = userService.getCurrentUser();
        if (user != null)
            return user.getUsername();
        else
            return null;
    }

    /**
     * applies url decode and adds leading slash
     *
     * @param path
     * @return
     */
    private String normalizePath(String path) {
        String normalizedPath = "";

        try {
            normalizedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("Problem with UTF-8");
        }

        if (!normalizedPath.startsWith("/"))
            return "/" + normalizedPath;

        return normalizedPath;
    }

    private void assureUserBelongsToProject(String projectId) throws IOException {
        if (!projectService.userBelongsToProject(userService.getCurrentUser().getUsername(), projectId)) {
            throw new UnauthorizedException("User has no rights on Project");
        }
    }
}
