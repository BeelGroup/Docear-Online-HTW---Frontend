package models.project.persistance;

import com.google.common.base.Function;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.io.IOException;
import java.util.Iterator;

import static com.google.common.collect.Iterables.transform;

public abstract class EntityCursorBase<T> implements EntityCursor {
    private final DBCursor cursor;

    public EntityCursorBase(DBCursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }

    protected abstract T convert(DBObject dbObject);

    @Override
    public Iterator<T> iterator() {
        return transform(cursor, new Function<DBObject, T>() {
            @Override
            public T apply(DBObject dbObject) {
                return convert(dbObject);
            }
        }).iterator();
    }
}
