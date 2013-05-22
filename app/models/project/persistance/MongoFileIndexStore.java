package models.project.persistance;

import com.google.common.base.Function;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static models.mongo.MongoPlugin.*;
import static com.google.common.collect.Lists.newArrayList;


public class MongoFileIndexStore implements FileIndexStore {

    public static final BasicDBObject DEFAULT_PRESENT_FIELDS_PROJECT = presentFields("name", "authUsers", "revision");

    @Override
    public Project findProjectById(String id) throws IOException {
        final BasicDBObject query = queryById(id);
        final BasicDBObject projectBson = (BasicDBObject) projects().findOne(query, DEFAULT_PRESENT_FIELDS_PROJECT);
        return convertToProject(projectBson);
    }

    private Project convertToProject(BasicDBObject bson) {
        Project result = null;
        if (bson != null) {
            final String idFromBson = bson.get("_id").toString();
            final String name = bson.getString("name");
            final long revision = bson.getInt("revision");
            final List<String> authorizedUsers = getStringList(bson, "authUsers");
            result = new Project(idFromBson, name, revision, authorizedUsers);
        }
        return result;
    }

    @Override
    public Iterable<Project> findProjectsFromUser(String username) throws IOException {
        final BasicDBObject query = doc("authUsers", username);
        DBCursor cursor = projects().find(query, DEFAULT_PRESENT_FIELDS_PROJECT);
        return transform(cursor, new Function<DBObject, Project>() {
            @Override
            public Project apply(DBObject dbObject) {
                Project result = null;
                if (dbObject instanceof BasicDBObject) {
                    result = convertToProject((BasicDBObject) dbObject);
                }
                return result;
            }
        });
    }

    @Override
    public void addUserToProject(String id, String username) throws IOException {
        final BasicDBObject query = queryById(id);
        projects().update(query, doc("$addToSet", doc("authUsers", username)));
    }

    @Override
    public void removeUserFromProject(String id, String username) throws IOException {
        final BasicDBObject query = queryById(id);
        projects().update(query, doc("$pull", doc("authUsers", username)));
    }

    @Override
    public Project createProject(String name, String username) throws IOException {
        final int revision = -1;
        final ArrayList<String> authorizedUsers = newArrayList(username);
        final BasicDBObject document = doc("name", name).
                append("revision", revision).
                append("changes", new ArrayList<String>()).
                append("authUsers", authorizedUsers);
        projects().insert(document);
        final String id = document.get("_id").toString();
        return new Project(id, name, revision, authorizedUsers);
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
