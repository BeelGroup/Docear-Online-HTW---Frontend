package assets;

import base.DocearHttpTest;
import org.junit.Test;
import play.libs.WS;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class HandlebarsDeployTest extends DocearHttpTest {
    @Test
    public void testDeploymentOfNodeHandlebar() throws Exception {
        runInServer(new Loader());
    }

    private static class Loader implements Runnable {
        @Override
        public void run() {
            final String handleBarUrl = url("/assets/javascripts/views/templates.pre.min.js");
            final WS.Response response = WS.url(handleBarUrl).get().get();//for Java Actions are better methods available, but for assets this is necessary
            assertThat(response.getStatus()).isEqualTo(OK);
            assertThat(response.getBody()).contains("(function(){var template=Handlebars.template");
        }
    }
}
