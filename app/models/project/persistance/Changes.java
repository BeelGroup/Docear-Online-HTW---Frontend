package models.project.persistance;

import java.util.List;

public class Changes {
    private final List<String> changedPaths;

    public Changes(List<String> changedPaths) {
        this.changedPaths = changedPaths;
    }

    public List<String> getChangedPaths() {
        return changedPaths;
    }
}
