package base;

import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import play.Configuration;
import play.Play;
import play.libs.F;
import play.test.TestBrowser;

public abstract class DocearTest {
	
	@Rule
    public ScreenshotTestRule screenshotTestRule = new ScreenshotTestRule();
	
	protected static WebDriver driver;
	
	@BeforeClass
	public static void setUp(){
		
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
	protected static void gotoRoot() {
		if (driver == null){
			driver = BrowserManager.getWebDriver();
		}
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
		running(testServer(port()), BrowserManager.getDriverClass(), callback);
	}
	
	protected void runInServer(final Runnable runnable) {
		running(testServer(port()), runnable);
	}
	
	protected boolean requiredFeatureEnabled(String requiredFeature){
		List<String> lst = new LinkedList<String>();
		lst.add(requiredFeature);
		return requiredFeaturesEnabled(lst);
	}
	
	protected boolean requiredFeaturesEnabled(List<String> requiredFeatures){
		Configuration featureConf = Play.application().configuration().getConfig("application.features");
        Set<String> availableFeatures = featureConf.keys();
		for (String feature : requiredFeatures) {
			if (!availableFeatures.contains(feature) || !featureConf.getBoolean(feature)){
				return false;
			}
			//Assume.assumeTrue("Required Feature '"+feature+"' is not enabled in Config.",availableFeatures.contains(feature) && featureConf.getBoolean(feature));
        }
		return true;
	}
	
	class ScreenshotTestRule implements MethodRule {
		private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		
		@Override
	    public Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object o) {
	        return new Statement() {
	            @Override
	            public void evaluate() throws Throwable {
	                try {
	                    statement.evaluate();
	                } catch (Throwable t) {
	                	if (BrowserManager.getDriverClass().equals(FirefoxDriver.class)){
	                		captureScreenshot(frameworkMethod.getName());
	                	}
	                	//rethrow exception for JUnit
	                    throw t; 
	                }
	            }
	 
	            public void captureScreenshot(String fileName) {
	                try {
	                    new File("test-reports").mkdirs(); // Insure directory is there
	                    FileOutputStream out = new FileOutputStream("test-reports/" + currTime() + "_" + fileName + ".png");
	                    out.write(((TakesScreenshot) BrowserManager.getWebDriver()).getScreenshotAs(OutputType.BYTES));
	                    out.close();
	                } catch (Exception e) { }
	            }
	            
	            private String currTime() {
	                Date date = new Date(System.currentTimeMillis());
	                return formatter.format(date);
	            }
	        };
	    }
	}
}

