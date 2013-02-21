package features.mindmap.node;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import play.libs.F;
import play.test.TestBrowser;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;

import base.DocearHttpTest;

public class LoginTest extends DocearHttpTest {

    @Test
    public void loginLogoutTest() throws Exception {
        runInBrowser(new F.Callback<TestBrowser>() {
            @Override
            @SuppressWarnings("unchecked") //type system is in fluentlenium library broken
            public void invoke(final TestBrowser testBrowser) throws Throwable {            	WebClient webClient = webClient();
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

				assertThat(page.asText()).contains("Logout");
				
				HtmlAnchor logout = page.getFirstByXPath("//a[@href='/logout']");
				page = logout.click();
				webClient.waitForBackgroundJavaScriptStartingBefore(defaultWait() * 1000);
				
				assertThat(page.getElementById("login-form")).isNotNull();
				
            }
        });
    }
}
