package models.project.persistance;

import com.mongodb.BasicDBObject;
import org.apache.commons.lang.NotImplementedException;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.List;

import static models.mongo.MongoPlugin.*;


public class MongoFileIndexStore implements FileIndexStore {
    @Override
    public Project findById(String id) throws IOException {
        final BasicDBObject query = doc("_id", new ObjectId(id));
        final BasicDBObject fields = doc("name", 1).append("authUsers", 1).append("revision", 1);
        final BasicDBObject projectBson = (BasicDBObject) projects().findOne(query, fields);
        Project result = null;
        if (projectBson != null) {
            final String name = projectBson.getString("name");
            final long revision = projectBson.getInt("revision");
            final List<String> authorizedUsers = getStringList(projectBson, "authUsers");
            result = new Project(id, name, revision, authorizedUsers);
        }
        return result;
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
