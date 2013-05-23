package models.project.persistance;

import java.io.Closeable;

public interface EntityCursor<T> extends Closeable, Iterable<T> {
}
