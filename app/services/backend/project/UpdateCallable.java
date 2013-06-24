package services.backend.project;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import play.Logger;
import services.backend.project.persistance.EntityCursor;
import services.backend.project.persistance.FileIndexStore;
import services.backend.project.persistance.Project;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class UpdateCallable implements Callable<JsonNode> {
    private final FileIndexStore fileIndexStore;
    private final Map<String, Long> projectRevisionMap;
    private final Map<String, List<String>> projectUserMap;
    private final String username;
    private Semaphore semaphore = new Semaphore(0);
    private boolean hasBeenCalled = false;

    public UpdateCallable(FileIndexStore fileIndexStore, Map<String, Long> projectRevisionMap, Map<String, List<String>> projectUserMap, String username) throws IOException {
        super();
        this.fileIndexStore = fileIndexStore;
        this.projectRevisionMap = projectRevisionMap;
        this.projectUserMap = projectUserMap;
        this.username = username;

        if (hasAlreadyUpdates())
            semaphore.release();
    }

    public void send() {
        semaphore.release();
    }

    public boolean hasBeenCalled() {
        return hasBeenCalled;
    }

    private boolean hasAlreadyUpdates() throws IOException {
        final RevisionsResponse response = getRevisionsResponse();
        if (response.getDeletedProjects().size() > 0)
            return true;
        if (response.getNewProjects().size() > 0)
            return true;

        if (response.getUpdatedProjects().size() > 0)
            return true;

        return false;
    }

    @Override
    public JsonNode call() throws Exception {
        semaphore.acquireUninterruptibly();

        final RevisionsResponse response = getRevisionsResponse();

        hasBeenCalled = true;
        return new ObjectMapper().valueToTree(response);
    }

    private RevisionsResponse getRevisionsResponse() throws IOException {

        final EntityCursor<Project> projects = fileIndexStore.findProjectsFromUser(username);
        final Map<String, Long> projectRevisionMapCopy = new HashMap<String, Long>(projectRevisionMap);
        final Map<String, Long> updatedProjects = new HashMap<String, Long>();
        final Map<String, Long> newProjects = new HashMap<String, Long>();
        final List<String> deletedProjects = new ArrayList<String>();

        final Iterator<Project> currentProjectsIterator = projects.iterator();
        try {
            // get updated projects
            while (currentProjectsIterator.hasNext()) {
                final Project project = currentProjectsIterator.next();
                final List<String> users = project.getAuthorizedUsers();
                final String projectId = project.getId();
                final Long projectRevision = project.getRevision();

                if (projectRevisionMapCopy.containsKey(projectId)) {
                    final boolean newRevision = projectRevisionMapCopy.get(projectId) != projectRevision;
                    final boolean changeInUsers = !areListsEqual(projectUserMap.get(projectId), users);
                    if (newRevision || changeInUsers)
                        updatedProjects.put(projectId, projectRevision);
                    projectRevisionMapCopy.remove(projectId);
                } else {
                    newProjects.put(projectId, projectRevision);
                }

            }
        } finally {
            projects.close();
        }
        // Left over ids must have been removed from the user
        deletedProjects.addAll(projectRevisionMapCopy.keySet());

        Logger.debug("updated: " + updatedProjects.size() + "; new: " + newProjects.size() + "; deleted: " + deletedProjects.size());

        return new RevisionsResponse(updatedProjects, newProjects, deletedProjects);
    }

    private <T> boolean areListsEqual(List<T> list1, List<T> list2) {
        List<T> copyList2 = new ArrayList<T>(list2);
        for (T entry : list1) {
            if (copyList2.contains(entry))
                copyList2.remove(entry);
            else
                return false;
        }

        return copyList2.size() == 0;
    }

    private static class RevisionsResponse {
        private final Map<String, Long> updatedProjects;
        private final Map<String, Long> newProjects;
        private final List<String> deletedProjects;

        public RevisionsResponse(Map<String, Long> updatedProjects, Map<String, Long> newProjects, List<String> deletedProjects) {
            super();
            this.updatedProjects = updatedProjects;
            this.newProjects = newProjects;
            this.deletedProjects = deletedProjects;
        }

        public Map<String, Long> getUpdatedProjects() {
            return updatedProjects;
        }

        public Map<String, Long> getNewProjects() {
            return newProjects;
        }

        public List<String> getDeletedProjects() {
            return deletedProjects;
        }

    }
}