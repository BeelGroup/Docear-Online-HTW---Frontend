package services.backend.project.persistance;

import com.google.common.collect.Iterables;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static models.mongo.MongoPlugin.*;


@Profile("mongoFileIndexStore")
@Component
public class MongoFileIndexStore implements FileIndexStore {

    public static final BasicDBObject DEFAULT_PRESENT_FIELDS_PROJECT = presentFields("name", "authUsers", "revision");
    public static final BasicDBObject DEFAULT_PRESENT_FIELDS_FILE_METADATA = presentFields("revision", "path").append("revisions", doc("$slice", -1));

    @Override
    public Project findProjectById(String id) throws IOException {
        try {
            final BasicDBObject query = queryById(id);
            final BasicDBObject projectBson = (BasicDBObject) projects().findOne(query, DEFAULT_PRESENT_FIELDS_PROJECT);
            return convertToProject(projectBson);
        } catch (MongoException e) {
            throw new IOException(e);
        }
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
        try {
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
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void addUserToProject(String id, String username) throws IOException {
        try {
            final BasicDBObject query = queryById(id);
            projects().update(query, doc("$addToSet", doc("authUsers", username)));
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void removeUserFromProject(String id, String username) throws IOException {
        try {
            final BasicDBObject query = queryById(id);
            projects().update(query, doc("$pull", doc("authUsers", username)));
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Project createProject(String name, String username) throws IOException {
        try {
            final int revision = -1;
            final ArrayList<String> authorizedUsers = newArrayList(username);
            final BasicDBObject document = doc("name", name).
                    append("revision", revision).
                    append("changes", new ArrayList<String>()).
                    append("authUsers", authorizedUsers);
            projects().insert(document);
            final String id = document.get("_id").toString();
            upsertFile(id, FileMetaData.folder("/", false));// add root "/" as base entry
            return findProjectById(id);
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void upsertFile(String id, FileMetaData file) throws IOException {
        try {
            if (getMetaData(id, file.getPath()) == null) {
                insertFileWithoutRevisions(id, file.getPath());
            }
            final BasicDBObject updatedFileBson = addNewFileRevisionToFileDocument(id, file);
            updateProjectForNewFileRevision(id, file, updatedFileBson);
        } catch (MongoException e) {
            throw new IOException(e);
        }
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
        try {
            final BasicDBObject query = doc("project", new ObjectId(id)).append("path", path);
            final BasicDBObject fileBson = (BasicDBObject) files().findOne(query, DEFAULT_PRESENT_FIELDS_FILE_METADATA);
            return convertToFileMetaData(fileBson);
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    private FileMetaData convertToFileMetaData(BasicDBObject fileBson) {
        FileMetaData result = null;
        if (fileBson != null) {
            final BasicDBList revisions = (BasicDBList) fileBson.get("revisions");//the only element is the last revision
            final BasicDBObject revisionBson = (BasicDBObject) revisions.get(0);
            final boolean isDir = revisionBson.getBoolean("is_dir");
            final boolean isDeleted = revisionBson.getBoolean("is_deleted");
            final long revision = fileBson.getLong("revision");
            final String filePath = fileBson.getString("path");
            if (isDir) {
                result = FileMetaData.folder(filePath, isDeleted);
            } else {
                final String hash = revisionBson.getString("hash");
                final long bytes = revisionBson.getInt("bytes");
                result = FileMetaData.file(filePath, hash, bytes, isDeleted);
            }
            result.setRevision(revision);
        }
        return result;
    }

    @Override
    public EntityCursor<FileMetaData> getMetaDataOfDirectChildren(String id, String path, int max) throws IOException {
        try {
            final String folderPath = path.endsWith("/") ? path : path + "/";
            Pattern childrenOfFolderPattern = Pattern.compile("^" + (folderPath) + "[^/]+$");
            final BasicDBObject query = doc("path", childrenOfFolderPattern).append("project", new ObjectId(id));
            final DBCursor cursor = files().find(query, DEFAULT_PRESENT_FIELDS_FILE_METADATA);
            return new EntityCursorBase<FileMetaData>(cursor) {
                @Override
                protected FileMetaData convert(DBObject dbObject) {
                    FileMetaData result = null;
                    if (dbObject instanceof BasicDBObject) {
                        result = convertToFileMetaData((BasicDBObject) dbObject);
                    }
                    return result;
                }
            };
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Changes getProjectChangesSinceRevision(String id, long revision) throws IOException {
        try {
            final BasicDBObject query = doc("$match", queryById(id));
            final BasicDBObject selectOnlyChangesArray = doc("$project", presentFields("changes").append("_id", 0));
            final BasicDBObject unwindChangesArray = doc("$unwind", "$changes");
            final BasicDBObject skipCurrentAndPreviousRevisions = doc("$skip", revision + 1);
            final BasicDBObject groupPaths = doc("$group", doc("_id", 0).append("paths", doc("$addToSet", "$changes.path")));
            final AggregationOutput output = projects().aggregate(query, selectOnlyChangesArray, unwindChangesArray, skipCurrentAndPreviousRevisions, selectOnlyChangesArray, groupPaths);
            final Iterable<DBObject> aggregationResult = output.results();
            final BasicDBObject result = (BasicDBObject) Iterables.getFirst(aggregationResult, null);
            List<String> changedPaths = emptyList();
            if (result != null) {
                changedPaths = getStringList(result, "paths");
            }
            return new Changes(changedPaths);
        } catch (MongoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean userBelongsToProject(String username, String id) {
        //TODO should an occurring MongoException converted to an IOException?
        boolean belongs = false;
        if (username != null && id != null) {
            final BasicDBObject query = queryById(id).append("authUsers", username);
            belongs = projects().count(query) == 1;
        }
        return belongs;
    }
}
