package models.mongo;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBList;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import play.*;
import util.Input;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.mongodb.util.JSON;

import static models.mongo.MongoPlugin.mongoClient;

/**
 * A plugin to load example data in development and in testing, but not in production.
 * A fixture file is a JSON file with an array of objects that should be inserted into a collection.
 *
 * Configuration: set in application.conf:
 * mongo.fixtures=["<subfolders-of-conf>/<db-name>/<collection-name>.json"]
 *
 * For problems converting between BSON and JSON consult http://docs.mongodb.org/manual/reference/mongodb-extended-json/
 */
public class MongoFixturesPlugin extends Plugin {
    private final Application application;

    public MongoFixturesPlugin(Application application) {
        this.application = application;
    }

    public boolean enabled() {
        return application.configuration().getBoolean("mongo.enabled", false);
    }

    public void onStart() {
        final Configuration conf = Play.application().configuration();
        final List<String> fixtureFilePaths = conf.getStringList("mongo.fixtures", Lists.<String>newArrayList());
        for (final String path : fixtureFilePaths) {
            insertIntoMongo(path);
        }
    }

    public void onStop() {
    }

    private void insertIntoMongo(final String path) {
        Logger.info("loading fixture: " + path);
        final File fixtureFile = Play.application().getFile(path);
        final String collection = fixtureFile.getName().replace(".json", "");
        final String database = fixtureFile.getParentFile().getName();
        try {
            final DBCollection mongoCollection = mongoClient().getDB(database).getCollection(collection);
            final String collectionAsJsonString = Input.resourceToString(path);
            final BasicDBList docs = (BasicDBList) JSON.parse(collectionAsJsonString);
            for (final Object document : docs) {
                mongoCollection.insert((DBObject)document);
            }
        } catch (IOException e) {
            Logger.error("cannot insert fixtures", e);
        }
    }
}
