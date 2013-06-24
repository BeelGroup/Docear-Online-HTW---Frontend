package services.backend.project;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class HashBasedProjectServiceTest {

    @Test
    public void testSHA512FileInputStream() throws Exception {
        FileInputStream in = new FileInputStream("./test/resources/projects/Freeplane/logos/Freeplane_logo.png");
        HashBasedProjectService service = new HashBasedProjectService();
        String hash = service.sha512(in);
        assertThat(hash).isEqualTo("086a85fe1133c22730bff6cbe2ad809ccd4852a746f2c975e451cb54d91d9c3c678b949d479d1a3b2858259787f4efd14ae905c896e7df1a844fc09dbe142368");
    }
    
    @Test
    public void testSHA512ByteArrayInputStream() throws Exception {
        File file= new File("./test/resources/projects/Freeplane/logos/Freeplane_logo.png");
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
        HashBasedProjectService service = new HashBasedProjectService();
        String hash = service.sha512(byteArrayInputStream);
        assertThat(hash).isEqualTo("086a85fe1133c22730bff6cbe2ad809ccd4852a746f2c975e451cb54d91d9c3c678b949d479d1a3b2858259787f4efd14ae905c896e7df1a844fc09dbe142368");
    }
}
