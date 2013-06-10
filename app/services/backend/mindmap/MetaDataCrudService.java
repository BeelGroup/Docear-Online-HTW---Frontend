package services.backend.mindmap;

import models.project.persistance.EntityCursor;

import java.io.IOException;

public interface MetaDataCrudService {
    void upsert(MetaData metaData) throws IOException;

    MetaData find(String projectId, String mindMapResource) throws IOException;

    void delete(String projectId, String mindMapResource) throws IOException;

    EntityCursor<MetaData> findAll() throws IOException;
}
