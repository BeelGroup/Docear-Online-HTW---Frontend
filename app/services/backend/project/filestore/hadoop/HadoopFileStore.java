package services.backend.project.filestore.hadoop;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.Play;
import services.backend.project.filestore.FileStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
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

    public HadoopFileStore() throws IOException {
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

        //mocking file
        if (path.contains("431/f6c688f6acaf438fd02be3b487aed4407b4377d3bf2b8cbb7cc40e745282723a9c216d9b40f347fe984426ad98a92a6888022a9cea810997cad8e18856a2c")) {
            //check if file is in hadoop
            try {
                final DataInputStream dataInputStream = fileSystem.open(new Path(path));
                return dataInputStream;
            } catch (FileNotFoundException e) {
                if (path.contains("zipped"))
                    return new DataInputStream(Play.application().resourceAsStream("/fixtures/hadoop/files/zipped/new"));
                else
                    return new DataInputStream(Play.application().resourceAsStream("/fixtures/hadoop/files/raw/new"));
            }

        }

        return fileSystem.open(new Path(path));
    }

    @Override
    public void move(String fromPath, String toPath) throws IOException, FileNotFoundException {
        final Path from = new Path(fromPath);
        final Path to = new Path(toPath);
        fileSystem.moveFromLocalFile(from, to);
    }

    @Override
    public String toString() {
        return "HadoopFileStore{" +
                "fileSystem=" + fileSystem +
                '}';
    }
}
