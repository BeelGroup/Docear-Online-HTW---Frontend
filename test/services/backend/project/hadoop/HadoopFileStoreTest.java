package services.backend.project.hadoop;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import services.backend.project.filestore.FileStore;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.fest.assertions.Assertions.assertThat;

public class HadoopFileStoreTest {

    FileStore fileStore;

    @Before
    public void setUp() throws Exception {
        fileStore = new HadoopFileStore(new RawLocalFileSystem());
    }

    @After
    public void tearDown() throws Exception {
        fileStore = null;
    }

    @Test
    public void testFileWriting() throws Exception {
        final String path = "aFile.txt";
        final DataOutputStream outputStream = fileStore.create(path);
        final String fileContentTemplate = "Hello";
        outputStream.write(fileContentTemplate.getBytes());
        closeQuietly(outputStream);
        final DataInputStream inputStream = fileStore.open(path);
        final String fileContent = IOUtils.toString(inputStream);
        assertThat(fileContent).isEqualTo(fileContentTemplate);
        closeQuietly(inputStream);
    }
}
