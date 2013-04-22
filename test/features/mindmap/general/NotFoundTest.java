package features.mindmap.general;
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;

import org.junit.Test;

import play.libs.WS;
import base.DocearTest;

public class NotFoundTest extends DocearTest {

    public static final String MIME_JSON = "application/json";
    public static final String MIME_HTML = "text/html";

    @Test
    public void notFoundHtml() throws Exception {
        runInServer(new NotFoundCheck(MIME_HTML));
    }
    @Test
    public void notFoundJson() throws Exception {
        runInServer(new NotFoundCheck(MIME_JSON));
    }

    private static class NotFoundCheck implements Runnable {
        private final String mime;

        private NotFoundCheck(String mime) {
            this.mime = mime;
        }

        @Override
        public void run() {
            final String notExistingUrl = url("/never/there");
            final WS.Response response = WS.url(notExistingUrl).setHeader("Accept", mime).get().get();
            assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
            assertThat(response.getBody()).containsIgnoringCase("not found");
            assertThat(response.getHeader("Content-Type")).containsIgnoringCase(mime);
        }
    }
}
