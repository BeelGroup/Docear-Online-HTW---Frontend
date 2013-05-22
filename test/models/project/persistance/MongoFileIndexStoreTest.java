package models.project.persistance;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import mongo.MongoTest;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

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
        return doc("_id", new ObjectId("507f191e810c19729de860ea"));
    }

    @Deprecated
    public void testAddRevisionToProject() throws Exception {
        final BasicDBObject updatedFile = updateFileInfos();
        final BasicDBObject updatedProject = updateProjectWithNewFileVersion(updatedFile);
        final int newProjectRevision = updatedProject.getInt("revision");
        assertThat(newProjectRevision).isEqualTo(4);
    }

    private BasicDBObject updateProjectWithNewFileVersion(BasicDBObject updatedFile) {
        final int revisionOfFile = updatedFile.getInt("revision");
        final String pathOfFile = updatedFile.getString("_id.path");
        BasicDBObject revisionInfo = new BasicDBObject("changes", new BasicDBObject("path", pathOfFile).append("revision", revisionOfFile));
        final BasicDBObject settings = new BasicDBObject().
                append("$inc", new BasicDBObject("revision", 1)).
                append("$push", revisionInfo);
        return (BasicDBObject) projects().findAndModify(queryForExampleProject(), new BasicDBObject(), new BasicDBObject(), false, settings, true, false);
    }

    private BasicDBObject updateFileInfos() {
        final String newFileHash = "12abc";
        BasicDBObject revisionInfo = new BasicDBObject("revisions", new BasicDBObject("filehash", newFileHash).append("timestamp", new Date()));
        BasicDBObject settings = new BasicDBObject("$inc", new BasicDBObject("revision", 1)).
                append("$push", revisionInfo);
        BasicDBObject fields = new BasicDBObject().
                append("revision", 1).
                append("revisions", 1);
        final BasicDBObject sort = new BasicDBObject();
        final BasicDBObject updatedFile = (BasicDBObject) files().findAndModify(queryForExampleFile(), fields, sort, false, settings, true, false);
        assertThat(updatedFile.getString("revision")).isEqualTo("2");

        final BasicDBList revisions = (BasicDBList) updatedFile.get("revisions");
        final BasicDBObject latestRevision = (BasicDBObject) revisions.get(2);
        assertThat(latestRevision.getString("filehash")).isEqualTo(newFileHash);
        return updatedFile;
    }

    private BasicDBObject queryForExampleFile() {
        final BasicDBObject idField = new BasicDBObject("project", new ObjectId("507f191e810c19729de860ea")).
                append("path", "/README.md");
        final BasicDBObject query = new BasicDBObject("_id", idField);
        return query;
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
        final Iterable<Project> alexProjects = store.findProjectsFromUser("Alex");
        final List<String> projectNames = newArrayList();
        for (final Project project : alexProjects) {
            projectNames.add(project.getName());
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
    public void testUpsertFile() throws Exception {

    }

    @Test
    public void testGetMetaDataSingleFile() throws Exception {

    }

    @Test
    public void testGetMetaDataFolder() throws Exception {

    }
}
