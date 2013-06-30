package services.backend.project.persistance;

import java.util.List;

public class Project {
    private final String id;
    private final String name;
    private final long revision;
    private final List<String> authorizedUsers;

    Project(String id, String name, long revision, List<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
        this.id = id;
        this.name = name;
        this.revision = revision;
    }

    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getRevision() {
        return revision;
    }

    @Override
    public String toString() {
        return "Project{" +
                "authorizedUsers=" + authorizedUsers +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", revision=" + revision +
                '}';
    }
}
