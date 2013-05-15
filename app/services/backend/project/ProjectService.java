package services.backend.project;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

public interface ProjectService {
	Promise<InputStream> getFile(String projectId, String path) throws IOException;
	Promise<JsonNode> metadata(String projectId, String path) throws IOException;
	Promise<JsonNode> createFolder(String projectId, String path) throws IOException;
	Promise<JsonNode> putFile(String projectId, String path, byte[] content) throws IOException;
	
	Promise<Boolean> listenIfUpdateOccurs(String projectId) throws IOException;
	Promise<String> versionDelta(String projectId, String cursor) throws IOException;
}
