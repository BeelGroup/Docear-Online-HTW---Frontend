package controllers;

import java.io.InputStream;

import models.project.formdatas.CreateFolderData;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.data.Form;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import services.backend.project.ProjectService;

@Component
public class ProjectController extends Controller {
	final Form<CreateFolderData> createFolderForm = Form.form(CreateFolderData.class);

	@Autowired
	private ProjectService projectService;

	public Result getFile(Long projectId, String path) {
		return async(projectService.getFile(projectId, path).map(new Function<InputStream, Result>() {

			@Override
			public Result apply(InputStream fileStream) throws Throwable {
				return ok(fileStream);
			}
		}));
	}

	public Result putFile(Long projectId, String path) {
		final byte[] content = request().body().asRaw().asBytes();
		return async(projectService.putFile(projectId, path, content).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode fileMeta) throws Throwable {
				return ok(fileMeta);
			}
		}));
	}

	public Result createFolder() {
		Form<CreateFolderData> filledForm = createFolderForm.bindFromRequest();

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final CreateFolderData data = filledForm.get();
			return async(projectService.createFolder(data.getProjectId(), data.getPath()).map(new Function<JsonNode, Result>() {
				@Override
				public Result apply(JsonNode folderMetadata) throws Throwable {
					return ok(folderMetadata);
				}
			}));
		}
	}

	public Result metadata(Long projectId, String path) {
		return async(projectService.metadata(projectId, path).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode entry) throws Throwable {
				return ok(entry);
			}
		}));
	}

	public Result listenForUpdates(Long projectId) {
		return async(projectService.listenIfUpdateOccurs(projectId).map(new Function<Boolean, Result>() {

			@Override
			public Result apply(Boolean hasChanged) throws Throwable {
				if (hasChanged)
					return ok();
				else
					return status(NOT_MODIFIED);
			}
		}));
	}

	public Result getUpdatesSince(Long projectId, Integer revision) {
		return async(projectService.getUpdatesSince(projectId, revision).map(new Function<String, Result>() {

			@Override
			public Result apply(String updates) throws Throwable {
				return ok(updates);
			}
		}));
	}

}
