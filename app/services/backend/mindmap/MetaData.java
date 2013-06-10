package services.backend.mindmap;

public final class MetaData {
    private final String projectId;
    private final String mindMapResource;
    private final Long lastSaved;
    private final Long currentRevision;

    public MetaData(final String projectId, final String mindMapResource, final Long currentRevision, final Long lastSaved) {
        this.currentRevision = currentRevision;
        this.lastSaved = lastSaved;
        this.mindMapResource = mindMapResource;
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getMindMapResource() {
        return mindMapResource;
    }

    public Long getLastSaved() {
        return lastSaved;
    }

    public Long getCurrentRevision() {
        return currentRevision;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaData metaData = (MetaData) o;

        if (currentRevision != null ? !currentRevision.equals(metaData.currentRevision) : metaData.currentRevision != null)
            return false;
        if (lastSaved != null ? !lastSaved.equals(metaData.lastSaved) : metaData.lastSaved != null) return false;
        if (mindMapResource != null ? !mindMapResource.equals(metaData.mindMapResource) : metaData.mindMapResource != null)
            return false;
        if (projectId != null ? !projectId.equals(metaData.projectId) : metaData.projectId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = projectId != null ? projectId.hashCode() : 0;
        result = 31 * result + (mindMapResource != null ? mindMapResource.hashCode() : 0);
        result = 31 * result + (lastSaved != null ? lastSaved.hashCode() : 0);
        result = 31 * result + (currentRevision != null ? currentRevision.hashCode() : 0);
        return result;
    }
}
