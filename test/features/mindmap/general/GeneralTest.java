package features.mindmap.general;


import static org.fest.assertions.Assertions.assertThat;
import static helper.Assertions.*;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import play.libs.F.Callback;
import play.test.TestBrowser;
import base.DocearTest;

public class GeneralTest extends DocearTest{
    
    @Test
    @Ignore
    public void initPageTest() {
        runInBrowser( new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	gotoRoot();
            	
            	//Check Title
            	assertThat(driver.getTitle()).isEqualTo("Login");
            	
            	//Check menu
            	WebElement menu = driver.findElement(By.className("menu-container"));
            	//Check links to Help and Imprint
            	assertThat(menu.findElement(By.linkText("Imprint")).getAttribute("href")).isEqualTo("http://www.docear.org/imprint/");
            	assertThat(menu.findElement(By.linkText("Help")).getAttribute("href")).isEqualTo(url("/help"));
            }
        });
    }
    
    @Test
    @Ignore
    public void initMapTest() {
        runInBrowser( new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	gotoRoot();
            	
            	//Check if no node is selected
            	List<WebElement> selectedNodes = driver.findElements(By.className("selected"));
            	assertThat(selectedNodes).isEmpty();
            	assertElementExistsNot(driver, By.className("selected"));
            	
            	//Check if exactly one root node exists
            	List<WebElement> rootNodes = driver.findElements(By.className("root"));
            	assertThat(rootNodes.size()).isEqualTo(1);
            	
            	if (rootNodes.size() == 1){
            		WebElement rootNode = rootNodes.get(0);
            		//Check for one collection of right children
            		assertElementExistsOnce(rootNode, By.className("rightChildren"));
            		//Check for one collection of left children
            		assertElementExistsOnce(rootNode, By.className("leftChildren"));
            		
            		//Check for node content
            		//assertThat(rootNode.findElement(By.xpath("//div[@class='inner-node']/div[@class='content']")).getText()).isEqualTo("Welcome to Docear Online (Alpha)");
            		assertThat(rootNode.findElement(By.cssSelector("div.root > div.inner-node > div.content")).getText()).isEqualTo("Welcome to Docear Online (Alpha)");
            	}
            	
            	//Check if all nodes are available
            	assertElementCountEquals(driver, By.cssSelector("div.node.left"), 11);
            	assertElementCountEquals(driver, By.cssSelector("div.node.right"), 10);
            }
        });
    }
}

