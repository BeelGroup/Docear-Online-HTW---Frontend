package services.backend.project;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;
import services.backend.project.persistance.EntityCursor;
import services.backend.project.persistance.FileMetaData;
import services.backend.project.persistance.Project;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The defined projectservice-API is inspired by the Dropbox-API
 * (https://www.dropbox.com/developers/core/docs).<br>
 * 
 */
public interface ProjectService {
	
	//Project level
    Project createProject(String username, String name) throws IOException;
	void addUserToProject(String projectId, String usernameToAdd) throws IOException;
	void removeUserFromProject(String projectId, String usernameToRemove) throws IOException;
    List<Project> getProjectsFromUser(String username) throws IOException;
	Project getProjectById(String projectId) throws IOException;
	
	
	
	//File level
	InputStream getFile(String projectId, String path) throws IOException;

    FileMetaData metadata(String projectId, String path) throws IOException;
    EntityCursor<FileMetaData> getMetaDataOfDirectChildren(String id, String path, int max) throws IOException;

    FileMetaData createFolder(String projectId, String path) throws IOException;

    FileMetaData putFile(String projectId, String path, byte[] fileBytes, boolean isZip, Long revision, boolean forceOverride) throws IOException;

	void moveFile(String projectId, String oldPath, String newPath) throws IOException;

    FileMetaData delete(String projectId, String path) throws IOException;

	Promise<JsonNode> listenIfUpdateOccurs(String username, Map<String, Long> projectRevisionMap) throws IOException;

    VersionDeltaResponse versionDelta(String projectId, Long revision) throws IOException;

    /**
     * Checks if the user with the name {@code username} belongs to the project.
     * @param username
     * @param projectId
     * @return true if the user belongs to the project, false otherwise including the project or the user is null or not existing
     */
    boolean userBelongsToProject(String username, String projectId);
}
