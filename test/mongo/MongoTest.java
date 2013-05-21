package mongo;

import com.google.common.collect.Maps;
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

import java.util.HashMap;

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
        final DBCollection projects = MongoPlugin.db().getCollection("projects");
        BasicDBObject query = (BasicDBObject) new BasicDBObject().put("_id", new ObjectId("507f191e810c19729de860ea"));
        final DBObject project = projects.findOne(query);
        assertThat(project.get("name").toString()).isEqualTo("Mein Projekt");
    }
}
