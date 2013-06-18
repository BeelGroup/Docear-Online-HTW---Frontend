package services.backend.project.persistance;

import java.io.Closeable;

public interface EntityCursor<T> extends Closeable, Iterable<T> {
}
