package services.backend.project;

import services.backend.project.persistance.FileMetaData;

import java.util.Map;

public class VersionDeltaResponse {
    private final Long currentRevision;
    private final Map<String, FileMetaData> resources;

    public VersionDeltaResponse(Long currentRevision, Map<String, FileMetaData> resources) {

        this.currentRevision = currentRevision;
        this.resources = resources;
    }

    public Long getCurrentRevision() {
        return currentRevision;
    }

    public Map<String, FileMetaData> getResources() {
        return resources;
    }
}
