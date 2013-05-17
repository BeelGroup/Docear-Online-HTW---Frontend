package services.backend.project.filestore.hadoop;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import services.backend.project.filestore.FileStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//import org.apache.hadoop.fs.FileSystem;

/**
 * A file store implementation via Hadoop HDFS.
 * http://hadoop.apache.org/docs/r0.23.7/api/index.html
 */
@Profile("hadoopFileStore")
@Component
public class HadoopFileStore implements FileStore {
    @Autowired
    private FileSystem fileSystem;
    
    public HadoopFileStore() {
    }

    public HadoopFileStore(FileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
    }


    @Override
    public DataOutputStream create(String path) throws IOException {
        final Path convertedPath = new Path(path);
        return fileSystem.create(convertedPath);
    }

    @Override
    public DataInputStream open(String path) throws IOException {
        return fileSystem.open(new Path(path));
    }

    @Override
    public String toString() {
        return "HadoopFileStore{" +
                "fileSystem=" + fileSystem +
                '}';
    }
}
