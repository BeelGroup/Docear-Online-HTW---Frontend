package base;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;


public class BrowserManager {
	private static Class<? extends WebDriver> driverClass;
	private static BrowserManager instance;
	private static WebDriver driver;
	
	private BrowserManager(String driverAlias){
		System.out.println("Creating BrowserDriver.");
		System.out.println(System.getProperty("browser"));
		if (driverAlias.equals("IE")
				&& System.getProperty("os.name").startsWith("Windows")) {
			createIE();
		} else if (driverAlias.equals("CHROME")) {
			createChrome();
		} else if (driverAlias.equals("FIREFOX")){
			createFirefox();
		} else {
			createHTMLUnitDriver();
		}
		
		driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
	}
	
	public static BrowserManager getInstance(String driverAlias){
		if (instance == null){
			instance = new BrowserManager(driverAlias);	
		}
		return instance;
	}
	
	public static BrowserManager getInstance(){
		return instance;
	}
	
	public static Class<? extends WebDriver> getDriverClass(){
		return driverClass;
	}
	
	public static WebDriver getWebDriver(){
		return driver;
	}

	private static void createHTMLUnitDriver() {
		driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_3_6);
		((HtmlUnitDriver) driver).setJavascriptEnabled(true);
		driverClass = HtmlUnitDriver.class;
	}
	
	private static void createIE() {
		driver = new InternetExplorerDriver();
		driverClass = InternetExplorerDriver.class;
	}

	private static void createChrome(){
		/*service = new ChromeDriverService.Builder()
        	.usingDriverExecutable(new File("C:/Users/Alexander/AppData/Local/Google/Chrome/Application/chrome.exe"))
        	.usingPort(port())
        	.build();
		try {
			service.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		driver = new ChromeDriver(service);*/
	    //driver = new RemoteWebDriver(service.getUrl(),DesiredCapabilities.chrome());
		//driver = new ChromeDriver();
		//System.setProperty("webdriver.chrome.driver", "C:/Users/Alexander/AppData/Local/Google/Chrome/Application/chrome.exe");
		driver = new ChromeDriver();
		driverClass = ChromeDriver.class;
	}

	private static void createFirefox() {
		FirefoxProfile profile = new FirefoxProfile();
		profile.setPreference( "browser.link.open_newwindow.restriction", 1);
		driver = new FirefoxDriver(profile);
		driver.manage().window().maximize();
		driverClass = FirefoxDriver.class;
	}
}
