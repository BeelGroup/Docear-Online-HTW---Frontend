package features.mindmap.node;

import base.DocearHttpTest;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Predicate;
import org.junit.Test;
import play.libs.F;
import play.test.TestBrowser;

import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.fluentlenium.FluentLeniumAssertions.assertThat;
import static org.fest.assertions.Assertions.assertThat;

public class VisualizationTest extends DocearHttpTest {

    @Test
    public void testText() throws Exception {
        runInBrowser(new F.Callback<TestBrowser>() {
            @Override
            @SuppressWarnings("unchecked") //type system is in fluentlenium library broken
            public void invoke(final TestBrowser testBrowser) throws Throwable {     		
            	WebClient webClient = webClient();

				HtmlPage page = webClient.getPage(url("/"));
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait());
				HtmlElement loadMapLink = page.getFirstByXPath("//a[@id='load-map-5']");
				page = loadMapLink.click();
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait() * 1000); // since
																							// millis

				HtmlElement mindmapContainer = page.getFirstByXPath("//div[@id='mindmap']");

				assertThat(mindmapContainer.asXml()).contains("right_L2P01_NodeLink_ToParent");
			}
        });
    }
}
