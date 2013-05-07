package services.backend.project;

import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;



public interface ProjectService {
	
	Promise<InputStream> getFile(Long projectId, String path);
	Promise<JsonNode> metadata(Long projectId, String path);
	Promise<JsonNode> createFolder(Long projectId, String path);
	Promise<JsonNode> putFile(Long projectId, String path, byte[] content);
	
	Promise<Boolean> listenIfUpdateOccurs(Long projectId);
	Promise<String> versionDelta(Long projectId, String cursor);
}
