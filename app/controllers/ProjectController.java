package controllers;

import java.io.InputStream;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import services.backend.project.ProjectService;

@Component
public class ProjectController extends Controller {

	@Autowired
	private ProjectService projectService;

	public Result listProject(Long projectId) {
		return async(projectService.listProject(projectId).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode project) throws Throwable {
				return ok(project);
			}
		}));
	}

	public Result listFolder(Long projectId, String path) {
		return async(projectService.listFolder(projectId, path).map(new Function<JsonNode, Result>() {

			@Override
			public Result apply(JsonNode project) throws Throwable {
				return ok(project);
			}
		}));
	}

	public Result getFile(Long projectId, String path) {
		return async(projectService.getFile(projectId, path).map(new Function<InputStream, Result>() {

			@Override
			public Result apply(InputStream fileStream) throws Throwable {
				return ok(fileStream);
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
