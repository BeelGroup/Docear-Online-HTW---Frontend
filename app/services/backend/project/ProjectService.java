package services.backend.project;

import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

public interface ProjectService {
	
	Promise<JsonNode> listProject(Long projectId);
	Promise<JsonNode> listFolder(Long projectId, String path);
	Promise<InputStream> getFile(Long projectId, String path);
	
	Promise<Boolean> listenIfUpdateOccurs(Long projectId);
	Promise<String> getUpdatesSince(Long projectId, Integer sinceRevision);
}
