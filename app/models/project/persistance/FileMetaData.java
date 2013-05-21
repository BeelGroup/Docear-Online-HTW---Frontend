package models.project.persistance;

public class FileMetaData {
    private final String path;
    private final String fileHash;
    private final long bytes;
    private final boolean isFolder;
    private final boolean isDeleted;
    private Long revision;

    FileMetaData(String path, String fileHash, long bytes, boolean isFolder, boolean isDeleted) {
        this.fileHash = fileHash;
        this.path = path;
        this.bytes = bytes;
        this.isFolder = isFolder;
        this.isDeleted = isDeleted;
    }

    public static FileMetaData folder(String path, boolean isDeleted) {
        return new FileMetaData(path, null, 0, true, isDeleted);
    }

    public static FileMetaData file(String path, String fileHash, long fileSize, boolean isDeleted) {
        return new FileMetaData(path, fileHash, fileSize, true, isDeleted);
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getBytes() {
        return bytes;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public String getPath() {
        return path;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
