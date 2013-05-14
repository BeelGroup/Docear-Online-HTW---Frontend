package base;

import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import play.Configuration;
import play.Play;
import play.libs.F;
import play.test.TestBrowser;

import com.gargoylesoftware.htmlunit.BrowserVersion;

public abstract class DocearTest {

	protected static WebDriver driver;
	protected static Class<? extends WebDriver> driverClass;

	@BeforeClass
	public static void setUp() {
		driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_3_6);
		((HtmlUnitDriver) driver).setJavascriptEnabled(true);
		driverClass = HtmlUnitDriver.class;
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
	protected static String url(final String path) {
		return "http://localhost:" + port() + path;
	}

	/**
	 * Generates URL for root page. http://localhost:3333/
	 * 
	 * @return the complete URL with the path.
	 */
	protected static void gotoRoot() {
		driver.get(url("/"));
	}

	protected static void gotoURL(String path) {
		driver.get(url(path));
	}

	/*
	 * maybe this could come from a configuration or environment variable, so
	 * leave it as function
	 */
	protected static int port() {
		return 9000;
	}

	protected void runInBrowser(final F.Callback<TestBrowser> callback) {
		running(testServer(port()), driverClass, callback);
	}

	protected void runInServer(final Runnable runnable) {
		running(testServer(port()), runnable);
	}

	protected boolean requiredFeatureEnabled(String requiredFeature) {
		List<String> lst = new LinkedList<String>();
		lst.add(requiredFeature);
		return requiredFeaturesEnabled(lst);
	}

	protected boolean requiredFeaturesEnabled(List<String> requiredFeatures) {
		Configuration featureConf = Play.application().configuration()
				.getConfig("application.features");
		Set<String> availableFeatures = featureConf.keys();
		for (String feature : requiredFeatures) {
			if (!availableFeatures.contains(feature)
					|| !featureConf.getBoolean(feature)) {
				return false;
			}
		}
		return true;
	}
}
