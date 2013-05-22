package models.project.persistance;

import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;

public class MongoFileIndexStore implements FileIndexStore {
    @Override
    public Project findById(String id) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public Iterable<Project> findProjectsFromUser(String username) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public void addUserToProject(String id, String username) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public void removeUserFromProject(String id, String username) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public Project createProject(String name, String username) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public void upsertFile(String id, FileMetaData file) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public FileMetaData getMetaData(String id, String path) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public Iterable<FileMetaData> getMetaData(String id, String path, int max) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }
}
