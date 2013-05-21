package mongo;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import models.mongo.MongoPlugin;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.test.FakeApplication;
import play.test.WithApplication;

import play.test.Helpers;

import java.util.Date;
import java.util.HashMap;

import static java.lang.Integer.parseInt;
import static models.mongo.MongoPlugin.files;
import static models.mongo.MongoPlugin.projects;
import static org.fest.assertions.Assertions.assertThat;

public class MongoTest extends WithApplication {
    @Before
    public void setUp() throws Exception {
        final HashMap<String,Object> additionalConfiguration = Maps.newHashMap();
        additionalConfiguration.put("embed.mongo.enabled", "true");
        additionalConfiguration.put("mongo.enabled", true);
        FakeApplication app = Helpers.fakeApplication(additionalConfiguration);
        start(app);
    }

    @After
    public void tearDown() throws Exception {
        stopPlay();
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
        return new BasicDBObject("_id", new ObjectId("507f191e810c19729de860ea"));
    }

    @Test
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
}
