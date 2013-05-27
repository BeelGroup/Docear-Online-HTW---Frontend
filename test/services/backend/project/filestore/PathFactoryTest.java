package services.backend.project.filestore;

import org.junit.Test;
import static services.backend.project.filestore.PathFactory.path;
import static org.fest.assertions.Assertions.assertThat;

public class PathFactoryTest {


    public static final String HASH = "sajisajijdasijd";

    @Test
    public void testZippedStore() throws Exception {
        final String path = path().hash(HASH).zipped();
        assertThat(path).isEqualTo("zipped/saj/isajijdasijd");
    }

    @Test
    public void testRawStore() throws Exception {
        final String path = path().hash(HASH).raw();
        assertThat(path).isEqualTo("raw/saj/isajijdasijd");
    }

    @Test
    public void testTmpStore() throws Exception {
        final String path1 = path().tmp();
        final String path2 = path().tmp();
        String basePath = "tmp/";
        assertThat(path1).startsWith(basePath);
        assertThat(path1.length()).isGreaterThan(basePath.length());
        assertThat(path2).startsWith(basePath);
        assertThat(path2.length()).isGreaterThan(basePath.length());
        assertThat(path2).overridingErrorMessage("two tmp paths should be different").isNotEqualTo(path1);
    }
}
