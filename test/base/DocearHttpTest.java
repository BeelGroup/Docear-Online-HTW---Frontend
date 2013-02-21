package base;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.google.common.collect.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import play.libs.F;
import play.test.TestBrowser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static play.test.Helpers.*;

public abstract class DocearHttpTest {
	/*
	 * maybe this could come from a configuration or environment variable, so
	 * leave it as function
	 */
	protected static final int port() {
		return 3333;
	}

	/** default waiting time in seconds **/
	protected int defaultWait() {
		return 3;
	}

	protected final void runInServer(final Runnable runnable) {
		running(testServer(port()), runnable);
	}

	protected final void runInBrowser(final F.Callback<TestBrowser> callback) {
		running(testServer(port()), driver(), callback);
	}

	private Class<? extends WebDriver> driver() {
		final String driverAlias = System.getProperty("PLAY_WEBDRIVER",
				"HTMLUNIT");
		final Map<String, Class<? extends WebDriver>> map = newHashMap();
		map.put("FIREFOX", FIREFOX);// play -DPLAY_WEBDRIVER=FIREFOX test
		map.put("CHROME", ChromeDriver.class);
		Class<? extends WebDriver> driver = HTMLUNIT;
		if (map.containsKey(driverAlias)) {
			driver = map.get(driverAlias);
		}
		return driver;
	}

	/**
	 * Generates an URL for a test.
	 * 
	 * example: url("/hello/world.html") returns
	 * http://localhost:3333/hello/world.html
	 * 
	 * @param path
	 *            the absolute path of for the URL
	 * @return the complete URL with the path.
	 */
	protected static final String url(final String path) {
		return "http://localhost:" + port() + path;
	}

	protected static final WebClient webClient() {
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_10);
		webClient.getCookieManager().setCookiesEnabled(true);
		webClient.waitForBackgroundJavaScript(5000);

		webClient.setIncorrectnessListener(new IncorrectnessListener() {

			@Override
			public void notify(String arg0, Object arg1) {
				// TODO Auto-generated method stub

			}
		});
		webClient.setCssErrorHandler(new ErrorHandler() {

			@Override
			public void warning(CSSParseException arg0) throws CSSException {
				// TODO Auto-generated method stub

			}

			@Override
			public void fatalError(CSSParseException arg0) throws CSSException {
				// TODO Auto-generated method stub

			}

			@Override
			public void error(CSSParseException arg0) throws CSSException {
				// TODO Auto-generated method stub

			}
		});

		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {

			@Override
			public void timeoutError(HtmlPage arg0, long arg1, long arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void scriptException(HtmlPage arg0, ScriptException arg1) {
				// TODO Auto-generated method stub

			}

			@Override
			public void malformedScriptURL(HtmlPage arg0, String arg1,
					MalformedURLException arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void loadScriptError(HtmlPage arg0, URL arg1, Exception arg2) {
				// TODO Auto-generated method stub

			}
		});

		webClient.setHTMLParserListener(new HTMLParserListener() {
			
			@Override
			public void warning(String arg0, URL arg1, String arg2, int arg3, int arg4,
					String arg5) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void error(String arg0, URL arg1, String arg2, int arg3, int arg4,
					String arg5) {
				// TODO Auto-generated method stub
				
			}
		});
		return webClient;
	}
}
