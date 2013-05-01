package services.backend.project;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import models.backend.Project;
import models.backend.ProjectEntry;
import models.backend.ProjectFolder;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;

@Profile("projectMock")
@Component
public class MockProjectService implements ProjectService {
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public Promise<JsonNode> listProject(Long projectId) {
		try {
			final File projectRoot = new File(Play.application().resource("rest/v1/project/" + projectId).toURI());
			final Project project = new Project(projectId, projectRoot);
			return Promise.pure(mapper.valueToTree(project));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<JsonNode> listFolder(Long projectId, String path) {
		if (!path.startsWith("/"))
			path = "/" + path;

		try {
			final File projectFolder = new File(Play.application().resource("rest/v1/project/" + projectId + "/files" + path).toURI());
			final ProjectEntry folder = new ProjectFolder("", path, projectFolder);
			return Promise.pure(mapper.valueToTree(folder));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<InputStream> getFile(Long projectId, String path) {
		if (!path.startsWith("/"))
			path = "/" + path;

		return Promise.pure(Play.application().resourceAsStream("rest/v1/project/" + projectId + "/files" + path));
	}

	@Override
	public Promise<Boolean> listenIfUpdateOccurs(Long projectId) {
		Promise<Boolean> promise = Akka.future(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Thread.sleep((long) (Math.random() * 30000));
				return true;
			}
		});

		return promise;
	}

	@Override
	public Promise<String> getUpdatesSince(Long projectId, Integer sinceRevision) {
		try {
			final File projectRoot = new File(Play.application().resource("rest/v1/project/" + projectId).toURI());
			return Promise.pure(Project.getUpdatesSinceJson(projectRoot, sinceRevision));

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
