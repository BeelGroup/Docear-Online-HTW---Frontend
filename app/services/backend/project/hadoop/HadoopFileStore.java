package services.backend.project.hadoop;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import services.backend.project.filestore.FileStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;

//import org.apache.hadoop.fs.FileSystem;

/**
 * A file store implementation via Hadoop HDFS.
 * http://hadoop.apache.org/docs/r0.23.7/api/index.html
 */
@Profile("hadoopFileStore")
@Component
public class HadoopFileStore implements FileStore {

    public HadoopFileStore() {
    }

    public HadoopFileStore(FileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
        final URI uri = fileSystem.getUri().resolve(new File("hadoop/fs/").getAbsolutePath());//TODO not suitable for prod, writes directly in working directory
        fileSystem.initialize(uri, new Configuration());
    }

    @Autowired
    private FileSystem fileSystem;

    @Override
    public DataOutputStream create(String path) throws IOException {
        final Path convertedPath = new Path(path);
        return fileSystem.create(convertedPath);
    }

    @Override
    public DataInputStream open(String path) throws IOException {
        return fileSystem.open(new Path(path));
    }
}
