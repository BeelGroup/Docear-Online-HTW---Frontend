package services.backend.mindmap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import services.backend.project.persistance.EntityCursor;
import services.backend.project.persistance.EntityCursorBase;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import static models.mongo.MongoPlugin.*;

@Profile("mindMapMetaDataMongo")
@Component
public class MongoMetaDataCrudService implements MetaDataCrudService {
    @Override
    public void upsert(MetaData metaData) throws IOException {
        final BasicDBObject document = toBson(metaData);
        mindMapMetaData().findAndModify(queryDocument(metaData), doc(), doc(), false, document, true, true);
    }

    private DBObject queryDocument(MetaData metaData) {
        return queryDocument(metaData.getProjectId(), metaData.getMindMapResource());
    }

    private BasicDBObject toBson(MetaData metaData) {
        return queryDocument(metaData.getProjectId(), metaData.getMindMapResource()).
                append("lastSaved", metaData.getLastSaved()).
                append("currentRevision", metaData.getCurrentRevision());
    }

    @Override
    public void delete(String projectId, String mindMapResource) throws IOException {
        final BasicDBObject query = queryDocument(projectId, mindMapResource);
        mindMapMetaData().remove(query);
    }

    @Override
    public MetaData find(String projectId, String mindMapResource) throws IOException {
        final BasicDBObject query = queryDocument(projectId, mindMapResource);
        final BasicDBObject result = (BasicDBObject) mindMapMetaData().findOne(query);
        return convertToPojo(result);
    }

    private MetaData convertToPojo(BasicDBObject bson) {
        MetaData result = null;
        if (bson != null) {
            final String projectId = bson.getString("projectId");
            final String mindMapResource = bson.getString("mindMapResource");
            final Long lastSaved = bson.getLong("lastSaved");
            final Long currentRevision = bson.getLong("currentRevision");
            result = new MetaData(projectId, mindMapResource, currentRevision, lastSaved);
        }
        return result;
    }

    private BasicDBObject queryDocument(String projectId, String mindMapResource) {
        return doc("projectId", projectId).append("mindMapResource", mindMapResource);
    }

    @Override
    public EntityCursor<MetaData> findAll() throws IOException {
        final DBCursor cursor = mindMapMetaData().find();
        return new EntityCursorBase<MetaData>(cursor) {
            @Override
            protected MetaData convert(DBObject dbObject) {
                MetaData result = null;
                if (dbObject instanceof BasicDBObject) {
                    result = convertToPojo((BasicDBObject) dbObject);
                }
                return result;
            }
        };
    }

    @Override
    public EntityCursor<MetaData> findByNotSavedSince(long millis) throws IOException {
        final BasicDBObject query = doc("lastSaved", doc("$gt", millis));
        final DBCursor cursor = mindMapMetaData().find(query);
        return new EntityCursorBase<MetaData>(cursor) {
            @Override
            protected MetaData convert(DBObject dbObject) {
                MetaData result = null;
                if (dbObject instanceof BasicDBObject) {
                    result = convertToPojo((BasicDBObject) dbObject);
                }
                return result;
            }
        };
    }
}
