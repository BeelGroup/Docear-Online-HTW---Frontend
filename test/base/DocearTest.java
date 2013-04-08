package base;

import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import play.libs.F;
import play.test.TestBrowser;

public abstract class DocearTest {
	
	@Rule
    public ScreenshotTestRule screenshotTestRule = new ScreenshotTestRule();

	private static Class<? extends WebDriver> driverClass;
	protected static WebDriver driver;

	@BeforeClass
	public static void getBrowserInfo() {
		final String driverAlias = System.getProperties().getProperty("selenium.browser", "FIREFOX");

		if (driverAlias.equals("IE")
				&& System.getProperty("os.name").startsWith("Windows")) {
			createIE();
		} else if (driverAlias.equals("CHROME")) {
			createChrome();
		} else {
			createFirefox();
		}
		driver.manage().window().maximize();
		driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
	}

	private static void createIE() {
		driver = new InternetExplorerDriver();
		driverClass = InternetExplorerDriver.class;
	}

	private static void createChrome(){
		driver = new ChromeDriver();
		driverClass = ChromeDriver.class;
	}

	private static void createFirefox() {
		driver = new FirefoxDriver();
		driverClass = FirefoxDriver.class;
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
	 * Generates URL for root page.
	 * http://localhost:3333/
	 * @return the complete URL with the path.
	 */
	protected String rootURL() {
		return "http://localhost:" + port();
	}
	
	/*
	 * maybe this could come from a configuration or environment variable, so
	 * leave it as function
	 */
	protected static int port() {
		return 9000;
	}


	@AfterClass
	public static void closeDriver() {
		driver.close();
	}

	protected void runInBrowser(final F.Callback<TestBrowser> callback) {
		running(testServer(port()), driverClass, callback);
	}
	
	protected void runInServer(final Runnable runnable) {
		running(testServer(port()), runnable);
	}
	
	class ScreenshotTestRule implements MethodRule {
		@Override
	    public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object o) {
	        return new Statement() {
	            @Override
	            public void evaluate() throws Throwable {
	                try {
	                    statement.evaluate();
	                } catch (Throwable t) {
	                    captureScreenshot(frameworkMethod.getName());
	                    throw t; // rethrow to allow the failure to be reported to JUnit
	                }
	            }
	 
	            public void captureScreenshot(String fileName) {
	                try {
	                    new File("test-reports/").mkdirs(); // Insure directory is there
	                    FileOutputStream out = new FileOutputStream("test-reports/" + System.currentTimeMillis() + "_" + fileName + ".png");
	                    out.write(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
	                    out.close();
	                } catch (Exception e) {
	                    // No need to crash the tests if the screenshot fails
	                }
	            }
	        };
	    }
	}
}

