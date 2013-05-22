package models.project.persistance;

import java.io.IOException;

/**
 * An interface to persist index information of files.
 */
public interface FileIndexStore {
    /**
     * Finds a project by the project id.
     * @param id the id of the project
     * @return The project from the database or null
     * @throws IOException
     */
    Project findById(String id) throws IOException;

    /**
     * Finds the projects a user is associated.
     * Implementations can be lazy fetching from the database.
     * It is used as iterable to make it work in for loops.
     * Do not use the iterable twice.
     * @param username the name of the who belongs to projects
     * @return an iterable with projects of username
     * @throws IOException
     */
    Iterable<Project> findProjectsFromUser(String username) throws IOException;

    /**
     * Adds an user to an existing project.
     * @param id the id of the project
     * @param username the name of the user that should be added to the project
     * @throws IOException
     */
    void addUserToProject(String id, String username) throws IOException;

    /**
     * Removes a user from a project.
     * @param id the id of the project where the user should be removed
     * @param username the name of the use that should be removed from the project
     * @throws IOException
     */
    void removeUserFromProject(String id, String username) throws IOException;

    /**
     * Creates a new project for a user.
     * @param name the name of the new project
     * @param username the name of the user who belongs to the project
     * @return the project meta data
     * @throws IOException
     */
    Project createProject(String name, String username) throws IOException;

    /**
     * Creates a new file revision. If there is no entry it will be created.
     * @param id the id of the project the file is associated
     * @param file the metadata of the file
     * @throws IOException
     */
    void upsertFile(String id, FileMetaData file) throws IOException;

    /**
     * Retrieves the metadata of one file or folder.
     * @param id the id of the project the file is associated
     * @param path the absolute path of the file
     * @return the metadata of the file
     * @throws IOException
     */
    FileMetaData getMetaData(String id, String path) throws IOException;

    /**
     * Retrieves metadata of the children of a folder.
     * Do not use the iterable twice.
     * @param id the id of the associated project
     * @param path the path of the folder
     * @param max the maximal number of results
     * @return an iterable with all files and folders of the specified folder. The first element is the folder itself.
     * @throws IOException
     */
    Iterable<FileMetaData> getMetaData(String id, String path, int max) throws IOException;
}
