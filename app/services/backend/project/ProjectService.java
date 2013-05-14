package services.backend.project;

import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

public interface ProjectService {
	Promise<InputStream> getFile(String projectId, String path);
	Promise<JsonNode> metadata(String projectId, String path);
	Promise<JsonNode> createFolder(String projectId, String path);
	Promise<JsonNode> putFile(String projectId, String path, byte[] content);
	
	Promise<Boolean> listenIfUpdateOccurs(String projectId);
	Promise<String> versionDelta(String projectId, String cursor);
}
