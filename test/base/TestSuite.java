package base;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import assets.HandlebarsDeployTest;
import features.mindmap.general.GeneralTest;
import features.mindmap.general.NotFoundTest;
import features.mindmap.node.FoldingTest;
import features.mindmap.node.LoginTest;


/*
 * Use 'sbt "test-only base.TestSuite"' to run TestSuite
 */

@RunWith(Suite.class)
@SuiteClasses({
				NotFoundTest.class,
				HandlebarsDeployTest.class,
				GeneralTest.class,
				FoldingTest.class,
				LoginTest.class			
				})
public class TestSuite {
	
    @BeforeClass 
    public static void setUpClass() {      
		final String driverAlias = System.getProperties().getProperty("browser", "HTMLUNIT");
		BrowserManager.getInstance(driverAlias);
    }

    @AfterClass 
    public static void tearDownClass() {
        BrowserManager.getWebDriver().close();
        BrowserManager.getWebDriver().quit();
    }
}
