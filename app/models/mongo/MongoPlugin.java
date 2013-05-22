package models.mongo;

import com.mongodb.*;
import org.apache.commons.lang3.Validate;
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
        } catch (UnknownHostException e) {
            Logger.error("can't connect ");
        }
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
}
