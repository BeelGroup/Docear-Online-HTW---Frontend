package models.project.persistance;

import com.google.common.collect.Iterables;
import com.mongodb.*;
import org.apache.commons.lang.NotImplementedException;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static models.mongo.MongoPlugin.*;
import static com.google.common.collect.Lists.newArrayList;


@Profile("mongoFileIndexStore")
@Component
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
    public EntityCursor<Project> findProjectsFromUser(String username) throws IOException {
        final BasicDBObject query = doc("authUsers", username);
        final DBCursor cursor = projects().find(query, DEFAULT_PRESENT_FIELDS_PROJECT);
        return new EntityCursorBase<Project>(cursor) {
            @Override
            protected Project convert(DBObject dbObject) {
                Project result = null;
                if (dbObject instanceof BasicDBObject) {
                    result = convertToProject((BasicDBObject) dbObject);
                }
                return result;
            }
        };
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
        if (getMetaData(id, file.getPath()) == null) {
            insertFileWithoutRevisions(id, file.getPath());
        }
        final BasicDBObject updatedFileBson = addNewFileRevisionToFileDocument(id, file);
        updateProjectForNewFileRevision(id, file, updatedFileBson);
    }

    private void updateProjectForNewFileRevision(String id, FileMetaData file, BasicDBObject updatedFileBson) {
        final int fileRevision = updatedFileBson.getInt("revision");
        BasicDBObject revisionInfo = doc("changes", doc("path", file.getPath()).append("revision", fileRevision));
        final BasicDBObject settings = doc("$inc", doc("revision", 1)).append("$push", revisionInfo);
        final BasicDBObject fields = doc();
        final BasicDBObject sort = doc();
        projects().findAndModify(queryById(id), fields, sort, false, settings, true, false);
    }

    private BasicDBObject addNewFileRevisionToFileDocument(String id, FileMetaData file) {
        final DBObject fields = presentFields("revision", "revisions");
        final DBObject sort = doc();
        BasicDBObject revisionInfo = doc("revisions", doc("hash", file.getHash()).
                append("timestamp", new Date()).
                append("bytes", file.getBytes()).
                append("is_dir", file.isDir()).
                append("is_deleted", file.isDeleted())
        );
        final DBObject update = doc("$inc", doc("revision", 1)).append("$push", revisionInfo);
        return (BasicDBObject) files().findAndModify(queryForFile(id, file.getPath()), fields, sort, false, update, true, false);
    }

    private void insertFileWithoutRevisions(String id, String path) {
        final BasicDBObject newFileDocument = queryForFile(id, path).append("revision", -1);
        files().insert(newFileDocument);
    }

    @Override
    public FileMetaData getMetaData(String id, String path) throws IOException {
        final BasicDBObject query = doc("_id",
                doc("project", new ObjectId(id)).append("path", path)
        );
        final BasicDBObject fields = presentFields("revision").append("revisions", doc("$slice", -1));
        final BasicDBObject fileBson = (BasicDBObject) files().findOne(query, fields);
        FileMetaData result = null;
        if (fileBson != null) {
            final BasicDBList revisions = (BasicDBList) fileBson.get("revisions");//the only element is the last revision
            final BasicDBObject revisionBson = (BasicDBObject) revisions.get(0);
            final boolean isDir = revisionBson.getBoolean("is_dir");
            final boolean isDeleted = revisionBson.getBoolean("is_deleted");
            final long revision = fileBson.getLong("revision");

            if (isDir) {
                result = FileMetaData.folder(path, isDeleted);
            } else {
                final String hash = revisionBson.getString("hash");
                final long bytes = revisionBson.getInt("bytes");
                result = FileMetaData.file(path, hash, bytes, isDeleted);
            }
            result.setRevision(revision);
        }
        return result;
    }

    @Override
    public Iterable<FileMetaData> getMetaData(String id, String path, int max) throws IOException {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues/462");
    }

    @Override
    public Changes getProjectChangesSinceRevision(String id, long revision) throws IOException {
        final BasicDBObject query = doc("$match", queryById(id));
        final BasicDBObject selectOnlyChangesArray = doc("$project", presentFields("changes").append("_id", 0));
        final BasicDBObject unwindChangesArray = doc("$unwind", "$changes");
        final BasicDBObject skipPreviousRevisions = doc("$skip", revision);
        final BasicDBObject groupPaths = doc("$group", doc("_id", 0).append("paths", doc("$addToSet", "$changes.path")));
        final AggregationOutput output = projects().aggregate(query, selectOnlyChangesArray, unwindChangesArray, skipPreviousRevisions, selectOnlyChangesArray, groupPaths);
        final BasicDBObject result = (BasicDBObject) Iterables.getFirst(output.results(), null);
        return new Changes(getStringList(result, "paths"));
    }
}
