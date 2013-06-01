package models.project.persistance;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import mongo.MongoTest;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static models.mongo.MongoPlugin.*;
import static org.fest.assertions.Assertions.assertThat;

public class MongoFileIndexStoreTest extends MongoTest {
    public static final String PROJECT_ID = "507f191e810c19729de860ea";
    public static final String PROJECT_NAME = "Mein Projekt";
    FileIndexStore store;

    @Before
    public void setUpService() throws Exception {
        store = new MongoFileIndexStore();
    }

    @After
    public void tearDownService() throws Exception {
        store = null;
    }

    @Test
    public void testFixtures() throws Exception {
        final DBCollection projects = projects();
        final BasicDBObject project = (BasicDBObject) projects.findOne(queryForExampleProject());
        assertThat(project.getString("name")).isEqualTo("Mein Projekt");
        final DBObject file = files().findOne(queryForExampleFile());
        assertThat(file).isNotNull();
    }

    private BasicDBObject queryForExampleProject() {
        return doc("_id", new ObjectId(PROJECT_ID));
    }

    private BasicDBObject queryForExampleFile() {
        return queryForFile(PROJECT_ID, "/README.md");
    }

    @Test
    public void testFindById() throws Exception {
        final Project project = store.findProjectById(PROJECT_ID);
        assertThat(project.getName()).isEqualTo(PROJECT_NAME);
        assertThat(project.getId()).isEqualTo(PROJECT_ID);
        assertThat(project.getAuthorizedUsers()).isEqualTo(newArrayList("Alex", "Micha", "Julius"));
        assertThat(project.getRevision()).isGreaterThanOrEqualTo(3);
    }

    @Test
    public void testFindProjectsFromUser() throws Exception {
        final EntityCursor<Project> alexProjects = store.findProjectsFromUser("Alex");
        final List<String> projectNames = newArrayList();
        try {
            for (final Project project : alexProjects) {
                projectNames.add(project.getName());
            }
        } finally {
            alexProjects.close();
        }
        assertThat(projectNames).hasSize(2);
        assertThat(projectNames).isEqualTo(newArrayList(PROJECT_NAME, "Docear Sync"));
    }

    @Test
    public void testAddUserToProject() throws Exception {
        final String newAuthorizedUser = "Florian";
        assertThat(store.findProjectById(PROJECT_ID).getAuthorizedUsers()).excludes(newAuthorizedUser);
        store.addUserToProject(PROJECT_ID, newAuthorizedUser);
        assertThat(store.findProjectById(PROJECT_ID).getAuthorizedUsers()).contains(newAuthorizedUser);
    }

    @Test
    public void testRemoveUserFromProject() throws Exception {
        final String userToBeRemoved = "Micha";
        assertThat(store.findProjectById(PROJECT_ID).getAuthorizedUsers()).contains(userToBeRemoved);
        store.removeUserFromProject(PROJECT_ID, userToBeRemoved);
        assertThat(store.findProjectById(PROJECT_ID).getAuthorizedUsers()).excludes(userToBeRemoved);
    }

    @Test
    public void testCreateProject() throws Exception {
        final String newProjectName = "Docear 4Word";
        final String projectOwnerName = "Stefan";
        final String id = store.createProject(newProjectName, projectOwnerName).getId();
        final Project project = store.findProjectById(id);
        assertThat(project.getId()).isNotEmpty();
        assertThat(project.getName()).isEqualTo(newProjectName);
        assertThat(project.getAuthorizedUsers()).contains(projectOwnerName);
        assertThat(project.getRevision()).isEqualTo(-1);
    }

    @Test
    public void testInsertFile() throws Exception {
        final String path = "/src/main/java/Util" + UUID.randomUUID() + ".java";
        final String hash = "32432423";
        assertThat(store.getMetaData(PROJECT_ID, path)).isNull();
        final int bytes = 500;
        final boolean isDir = false;
        final boolean isDeleted = false;
        final FileMetaData metaData = new FileMetaData(path, hash, bytes, isDir, isDeleted);
        store.upsertFile(PROJECT_ID, metaData);
        final FileMetaData storeMetaData = store.getMetaData(PROJECT_ID, path);
        assertThat(storeMetaData).isNotNull();
        assertThat(storeMetaData.getPath()).isEqualTo(path);
        assertThat(storeMetaData.getHash()).isEqualTo(hash);
        assertThat(storeMetaData.getBytes()).isEqualTo(bytes);
        assertThat(storeMetaData.isDir()).isEqualTo(isDir);
        assertThat(storeMetaData.isDeleted()).isEqualTo(isDeleted);
        assertThat(store.getProjectChangesSinceRevision(PROJECT_ID, 0).getChangedPaths()).contains(path);
    }

    @Test
    public void testInsertFolder() throws Exception {

    }

    @Test
    public void testUpdateFile() throws Exception {

    }

    @Test
    public void testDeleteFile() throws Exception {

    }

    @Test
    public void testGetMetaDataSingleFile() throws Exception {
        final String filePath = "/README.md";
        final FileMetaData metaData = store.getMetaData(PROJECT_ID, filePath);
        assertThat(metaData.getPath()).isEqualTo(filePath);
        assertThat(metaData.getHash()).isEqualTo("122233a");
        assertThat(metaData.getBytes()).isEqualTo(1323);
        assertThat(metaData.isDir()).isEqualTo(false);
        assertThat(metaData.isDeleted()).isEqualTo(false);
        assertThat(metaData.getRevision()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void testGetMetaDataFolder() throws Exception {
        //since we test we can consume all at once
        final List<FileMetaData> data = newArrayList(store.getMetaDataOfDirectChildren(PROJECT_ID, "/src/main/java", 100));
        assertThat(data.size()).isGreaterThanOrEqualTo(2);
        final List<String> paths = Lists.transform(data, new Function<FileMetaData, String>() {
            @Override
            public String apply(FileMetaData fileMetaData) {
                return fileMetaData.getPath();
            }
        });
        assertThat(paths).contains("/src/main/java/models", "/src/main/java/Main.java");
        assertThat(paths.contains("/README.md")).overridingErrorMessage("should not contain elements from above folders").isFalse();
        assertThat(paths.contains("/src/main/java/models/Person.java")).overridingErrorMessage("should not return transitive child elements").isFalse();
    }

    @Test
    public void testGetProjectChangesSinceRevision() throws Exception {
        final Changes changes = store.getProjectChangesSinceRevision(PROJECT_ID, 1);
        assertThat(changes.getChangedPaths()).contains("/README.md", "/src/main/java/Main.java");
        assertThat(new HashSet<String>(changes.getChangedPaths())).hasSize(changes.getChangedPaths().size());
    }

    @Test
    public void testGetProjectChangesSinceRevisionForCurrentRevision() throws Exception {
        final Project project = store.findProjectById(PROJECT_ID);
        final long revision = project.getRevision();
        final Changes changes = store.getProjectChangesSinceRevision(PROJECT_ID, revision);
        assertThat(changes.getChangedPaths()).overridingErrorMessage("Given the participant is on the current project revision" +
                "then he should not receive any changes").hasSize(0);
    }

    @Test
    public void testUserBelongsToProject() throws Exception {
        assertThat(store.userBelongsToProject("Julius", PROJECT_ID)).isTrue();
        assertThat(store.userBelongsToProject("Nobody", PROJECT_ID)).isFalse();
    }
}
