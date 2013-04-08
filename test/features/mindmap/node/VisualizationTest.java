package features.mindmap.node;

import org.junit.Ignore;
import org.junit.Test;

import play.libs.F;
import play.test.TestBrowser;
import base.DocearTest;

public class VisualizationTest extends DocearTest {

	/**
	 * @deprecated Use Selenium Test instead
	 */
    @Test
    @Ignore
    @Deprecated
    public void testText() throws Exception {
        runInBrowser(new F.Callback<TestBrowser>() {
            @Override
            @SuppressWarnings("unchecked") //type system is in fluentlenium library broken
            public void invoke(final TestBrowser testBrowser) throws Throwable {     		
            	/*WebClient webClient = webClient();

				HtmlPage page = webClient.getPage(url("/"));
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait() * 1000);

				HtmlForm form = page.getFirstByXPath("//form[@id='login-form']");
				
				HtmlInput username = form.getFirstByXPath(".//input[@id='username']");
				username.type("Paul");

				HtmlPasswordInput password = form.getFirstByXPath(".//input[@id='password']");
				password.type("secret");
				
				HtmlButton login = form.getFirstByXPath(".//button[@type='submit']");
				
				page = login.click();
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait() * 1000); // since

				HtmlElement loadMapLink = page.getFirstByXPath("//ul[@id='select-mindmap']/li[2]/a");
				page = loadMapLink.click();
				
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait() * 1000); // since

				List nodes = page.getByXPath("//div[contains(@class, 'node')]");
				
				assertThat(nodes.size()).isGreaterThan(0);*/
			}
        });
    }
}
