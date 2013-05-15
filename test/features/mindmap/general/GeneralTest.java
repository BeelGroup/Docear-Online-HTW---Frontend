package features.mindmap.general;


import static helper.Assertions.assertElementCountEquals;
import static helper.Assertions.assertElementExistsNot;
import static helper.Assertions.assertElementExistsOnce;
import static org.fest.assertions.Assertions.assertThat;

import helper.Wait;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import controllers.featuretoggle.Feature;

import play.libs.F.Callback;
import play.test.TestBrowser;
import base.DocearTest;

public class GeneralTest extends DocearTest{
    
    @Test
    public void initPageTest() {
        runInBrowser( new Callback<TestBrowser>() {
            public void invoke(TestBrowser browser) {
            	gotoRoot();
            	
            	//Check Title
            	assertThat(driver.getTitle()).isEqualTo("Docear online (Alpha) - Login");
            	

            	//Check menu
            	WebElement menu;
				if (!requiredFeatureEnabled(Feature.RIBBONS.name())){
					menu = driver.findElement(By.className("menu-container"));
				} else {
					menu = driver.findElement(By.id("ribbons"));
				}

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
            	assertElementExistsNot(driver, By.className("selected"));
            	
            	//Check if exactly one root node exists
            	List<WebElement> rootNodes = driver.findElements(By.cssSelector("div.node.root"));
            	assertThat(rootNodes.size()).isEqualTo(1);
            	
            	if (rootNodes.size() == 1){
            		WebElement rootNode = rootNodes.get(0);
            		//Check for one collection of right children
            		assertElementExistsOnce(rootNode, By.className("rightChildren"));
            		//Check for one collection of left children
            		assertElementExistsOnce(rootNode, By.className("leftChildren"));
            		
            		//Check for node content
            		assertThat(rootNode.findElement(By.cssSelector("div.root > div.inner-node > div.content")).getText()).isEqualTo("Welcome to Docear Online (Alpha)");
            	}
            	
            	//Check if all nodes are available
            	assertElementCountEquals(driver, By.cssSelector("div.node.left"), 11);
            	assertElementCountEquals(driver, By.cssSelector("div.node.right"), 10);
            }
        });
    }
}

