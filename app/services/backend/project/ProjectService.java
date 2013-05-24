package services.backend.project;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

/**
 * The defined projectservice-API is inspired by the Dropbox-API
 * (https://www.dropbox.com/developers/core/docs).<br>
 * 
 */
public interface ProjectService {
	
	//Project level
	Promise<JsonNode> createProject(String username, String name) throws IOException;
	Promise<Boolean> addUserToProject(String username, String projectId, String usernameToAdd) throws IOException;
	Promise<Boolean> removeUserFromProject(String username, String projectId, String usernameToRemove) throws IOException;
	
	Promise<JsonNode> getProjectsFromUser(String username) throws IOException;
	
	/**
	 * 
	 * @param username important for user rights validation
	 * @param projectId id of the project
	 * @return
	 * @throws IOException
	 */
	Promise<JsonNode> getProjectById(String username, String projectId) throws IOException;
	
	
	
	//File level
	Promise<InputStream> getFile(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> metadata(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> createFolder(String username, String projectId, String path) throws IOException;

	Promise<JsonNode> putFile(String username, String projectId, String path, byte[] fileBytes, boolean isZip) throws IOException;

	Promise<JsonNode> delete(String username, String projectId, String path) throws IOException;

	Promise<Boolean> listenIfUpdateOccurs(String username, String projectId) throws IOException;

	Promise<JsonNode> versionDelta(String username, String projectId, Long cursor) throws IOException;

    /**
     * Checks if the user with the name {@code username} belongs to the project.
     * @param username
     * @param projectId
     * @return true if the user belongs to the project, false otherwise including the project or the user is null or not existing
     */
    boolean userBelongsToProject(String username, String projectId);
}
