package services.backend.project;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;
import services.backend.project.persistance.Project;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * The defined projectservice-API is inspired by the Dropbox-API
 * (https://www.dropbox.com/developers/core/docs).<br>
 * 
 */
public interface ProjectService {
	
	//Project level
    Project createProject(String username, String name) throws IOException;
	Promise<Boolean> addUserToProject(String projectId, String usernameToAdd) throws IOException;
	Promise<Boolean> removeUserFromProject(String projectId, String usernameToRemove) throws IOException;
	Promise<JsonNode> getProjectsFromUser(String username) throws IOException;
	
	/**
	 *
	 * @param projectId id of the project
	 * @return
	 * @throws IOException
	 */
	Promise<JsonNode> getProjectById(String projectId) throws IOException;
	
	
	
	//File level
	Promise<InputStream> getFile(String projectId, String path) throws IOException;

	Promise<JsonNode> metadata(String projectId, String path) throws IOException;

	Promise<JsonNode> createFolder(String projectId, String path) throws IOException;

	Promise<JsonNode> putFile(String projectId, String path, byte[] fileBytes, boolean isZip, Long revision, boolean forceOverride) throws IOException;

	Promise<JsonNode> moveFile(String projectId, String oldPath, String newPath) throws IOException;
	
	Promise<JsonNode> delete(String projectId, String path) throws IOException;

	Promise<JsonNode> listenIfUpdateOccurs(String username, Map<String, Long> projectRevisionMap) throws IOException;

	Promise<JsonNode> versionDelta(String projectId, Long revision) throws IOException;

    /**
     * Checks if the user with the name {@code username} belongs to the project.
     * @param username
     * @param projectId
     * @return true if the user belongs to the project, false otherwise including the project or the user is null or not existing
     */
    boolean userBelongsToProject(String username, String projectId);
}
