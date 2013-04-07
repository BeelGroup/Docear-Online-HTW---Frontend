package features.mindmap.general;


import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import play.libs.F.Callback;
import play.test.TestBrowser;
import base.DocearHttpTest;

public class GeneralTest extends DocearHttpTest{
    /*
    @Test
    public void initPageTest() {
        runInBrowser( new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	WebDriver driver = getDriver(browser);
            	
            	driver.get(rootURL());
            	
            	//Check Title
            	assertThat(driver.getTitle()).isEqualTo("Login");
            	
            	//Check menu
            	WebElement menu = driver.findElement(By.className("menu-container"));
            	
            	WebElement imprint = menu.findElement(By.linkText("Imprint"));
            	play.Logger.debug(imprint.toString());
            	assertThat(menu.findElement(By.linkText("Imprint")).getAttribute("href")).isEqualTo("http://www.docear.org/imprint/");
            	assertThat(menu.findElement(By.linkText("Help")).getAttribute("href")).isEqualTo(url("/help"));
            }
        });
    }*/
    
    @Test
    public void initMapTest() {
        runInBrowser( new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	WebDriver driver = getDriver(browser);
            	
            	driver.get(rootURL());
            	
            	//Check if no node is selected
            	List<WebElement> deleteLinks = driver.findElements(By.className("selected"));
            	assertThat(deleteLinks).isEmpty();
            	
            	//Check if exactly one root node exists
            	List<WebElement> rootNodes = driver.findElements(By.className("root"));
            	assertThat(rootNodes.size()).isEqualTo(1);
            
            	
            	if (rootNodes.size() == 1){
            		WebElement rootNode = rootNodes.get(0);
            		//Check for one collection of right children
            		assertThat(rootNode.findElements(By.className("rightChildren")).size()).isEqualTo(1);
            		//Check for one collection of left children
            		assertThat(rootNode.findElements(By.className("leftChildren")).size()).isEqualTo(1);
            		//Check for node content
            		
            		//assertThat(rootNode.findElement(By.xpath("//div[@class='inner-node']/div[@class='content']")).getText()).isEqualTo("Welcome to Docear Online (Alpha)");
            		assertThat(rootNode.findElement(By.cssSelector("div.root > div.inner-node > div.content")).getText()).isEqualTo("Welcome to Docear Online (Alpha)");
            	}
            	
            	
            	//Check if all nodes are available
            	assertThat(driver.findElements(By.cssSelector("div.node.left")).size()).isEqualTo(11);
            	assertThat(driver.findElements(By.cssSelector("div.node.right")).size()).isEqualTo(10);
            	
            }
        });
    }
}

