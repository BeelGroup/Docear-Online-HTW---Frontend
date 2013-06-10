package mongo;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;

import java.util.HashMap;

public abstract class MongoTest extends WithApplication {
    @Before
    public void setUpApplication() throws Exception {
        final HashMap<String, Object> additionalConfiguration = Maps.newHashMap();
        additionalConfiguration.put("embed.mongo.enabled", "true");
        additionalConfiguration.put("mongo.enabled", true);
        FakeApplication app = Helpers.fakeApplication(additionalConfiguration);
        start(app);
    }

    @After
    public void tearDownApplication() throws Exception {
        stopPlay();
    }
}
