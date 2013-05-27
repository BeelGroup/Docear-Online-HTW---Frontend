package models.mongo;

import com.mongodb.*;
import org.apache.commons.lang3.Validate;
import org.bson.types.ObjectId;
import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Connection Plugins.
 * See also http://docs.mongodb.org/ecosystem/tutorial/getting-started-with-java-driver/#getting-started-with-java-driver
 */
public class MongoPlugin extends Plugin {
    private final Application application;
    MongoClient mongoClient;

    public MongoPlugin(Application application) {
        this.application = application;
    }

    public boolean enabled() {
        return application.configuration().getBoolean("mongo.enabled", false);
    }

    public void onStart(){
        //with Mongo multiple host and ports are possible, but one is enough for now
        try {
            final String host = host();
            final Integer port = port();
            Logger.info(String.format("Starting MongoClient for %s:%d", host, port));
            mongoClient = new MongoClient(host, port);
            ensureIndexes();
        } catch (UnknownHostException e) {
            Logger.error("can't connect ");
        }
    }

    private void ensureIndexes() {
        files().ensureIndex(doc("project", 1).append("path", 1), doc("unique", true));
        projects().ensureIndex(doc("_id", 1).append("authUsers", 1));
    }

    private String host() {
        final String configKeyMongoHost = "mongo.host";
        final String host = application.configuration().getString(configKeyMongoHost);
        Validate.notNull(host, configKeyMongoHost + " is mandatory");
        return host;
    }

    private Integer port() {
        final String configKeyMongoPort = "mongo.port";
        final Integer port = application.configuration().getInt(configKeyMongoPort);
        Validate.notNull(port, configKeyMongoPort + " is mandatory");
        return port;
    }

    public static MongoClient mongoClient() {
        return Play.application().plugin(MongoPlugin.class).mongoClient;
    }

    public static DB db() {
        return mongoClient().getDB(Play.application().configuration().getString("mongo.db.default.name"));
    }

    public static DBCollection projects() {
        return db().getCollection("projects");
    }

    public static DBCollection files() {
        return db().getCollection("files");
    }

    public static BasicDBObject doc() {
        return new BasicDBObject();
    }

    public static BasicDBObject doc(final String key, final Object value) {
        return new BasicDBObject(key, value);
    }

    public static List<String> getStringList(final BasicDBObject bson, final String key) {
        final Object maybeList = bson.get(key);
        List<String> result = null;
        if (maybeList != null && maybeList instanceof BasicDBList) {
            final BasicDBList bsonList = (BasicDBList) maybeList;
            result = new ArrayList<String>(bsonList.size());
            for (final Object item : bsonList) {
                result.add(item.toString());
            }
        }
        return result;
    }

    /**
     * Creates a BSON document for the field selecting syntax.
     * @param fields the fields that should be in the document
     * @return
     */
    public static BasicDBObject presentFields(String... fields) {
        final BasicDBObject result = new BasicDBObject();
        for (final String field : fields) {
            result.append(field, 1);
        }
        return result;
    }

    public static BasicDBObject queryById(final String id) {
        return doc("_id", new ObjectId(id));
    }

    public static BasicDBObject queryForFile(String id, String path) {
        return doc("project", new ObjectId(id)).append("path", path);
    }
}
