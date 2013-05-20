package services.backend.project;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

/**
 * The defined projectservice-API is inspired by the Dropbox-API
 * (https://www.dropbox.com/developers/core/docs).<br>
 * 
 */
public interface ProjectService {
	Promise<InputStream> getFile(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> metadata(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> createFolder(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> putFile(String username, String projectId, String path, byte[] content) throws IOException;

	Promise<JsonNode> delete(String username, String projectId, String path) throws IOException;

	Promise<Boolean> listenIfUpdateOccurs(String username, String projectId) throws IOException;

	Promise<String> versionDelta(String username, String projectId, String cursor) throws IOException;

}
