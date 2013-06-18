package services.backend.project.persistance;

public class FileMetaData {
    private final String path;
    private final String hash;
    private final long bytes;
    private final boolean isDir;
    private final boolean isDeleted;
    private Long revision;

    FileMetaData(String path, String hash, long bytes, boolean isDir, boolean isDeleted) {
        this.hash = hash;
        this.path = path;
        this.bytes = bytes;
        this.isDir = isDir;
        this.isDeleted = isDeleted;
    }

    public static FileMetaData folder(String path, boolean isDeleted) {
        return new FileMetaData(path, null, 0, true, isDeleted);
    }

    public static FileMetaData file(String path, String hash, long bytes, boolean isDeleted) {
        return new FileMetaData(path, hash, bytes, false, isDeleted);
    }

    public String getHash() {
        return hash;
    }

    public long getBytes() {
        return bytes;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isDir() {
        return isDir;
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

    @Override
    public String toString() {
        return "FileMetaData{" +
                "bytes=" + bytes +
                ", path='" + path + '\'' +
                ", hash='" + hash + '\'' +
                ", isDir=" + isDir +
                ", isDeleted=" + isDeleted +
                ", revision=" + revision +
                '}';
    }
}
