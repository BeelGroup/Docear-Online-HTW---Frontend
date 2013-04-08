package features.mindmap.node;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import play.libs.F.Callback;
import play.test.TestBrowser;
import base.DocearTest;

public class FoldingTest extends DocearTest{
	@Test
	public void initTest() {
		runInBrowser(new Callback<TestBrowser>() {
			public void invoke(TestBrowser browser) {
				driver.get(rootURL());

				WebElement rootNode = driver.findElement(By.className("root"));

				// Check if all fold icons (+/- left/right) are invisible
				assertThat(rootNode.findElements(By.cssSelector("div.root > div.collapse-icon.invisible")).size()).isEqualTo(2);
				assertThat(rootNode.findElements(By.cssSelector("div.root > div.expand-icon.invisible")).size()).isEqualTo(2);
			}
		});
	}
	
	@Test
	public void mouseOverTest() {
		runInBrowser(new Callback<TestBrowser>() {
			public void invoke(TestBrowser browser) {
				driver.get(rootURL());

				WebElement rootNode = driver.findElement(By.className("root"));
				
				// Move Mouse to root node
				Actions mouseOver = new Actions(driver);
				mouseOver.moveToElement(rootNode);

				//Check if fold icons are visible
				assertThat(rootNode.findElements(By.cssSelector("div.root > div.collapse-icon.invisible")).size()).isEqualTo(0);
				//Check if unfold icons are invisible
				assertThat(rootNode.findElements(By.cssSelector("div.root > div.expand-icon.invisible")).size()).isEqualTo(2);
			}
		});
	}
	
	@Test
	public void selectNodeWithFoldIconsTest() {
		runInBrowser(new Callback<TestBrowser>() {
			public void invoke(TestBrowser browser) {			
				driver.get(rootURL());

				WebElement rootNode = driver.findElement(By.className("root"));
				WebElement nodeWithFoldIcon = rootNode.findElement(By.cssSelector("div.root > div.leftChildren > div.node"));
				
				//Click on node
				WebElement collapseIcon = nodeWithFoldIcon.findElement(By.cssSelector("div.node > div.inner-node > div.collapse-icon > img"));
				assertThat(collapseIcon.getCssValue("display")).isEqualTo("none");
				
				nodeWithFoldIcon.click();
				assertThat(collapseIcon.getCssValue("display")).isEqualTo("block");
				
				//Check if Collapse icon is visible
				//assertThat(nodeWithFoldIcon.findElements(By.cssSelector("div.node > div.inner-node > div.collapse-icon")).size()).isEqualTo(1);
				//assertThat(nodeWithFoldIcon.findElements(By.cssSelector("div.node > div.inner-node > div.collapse-icon.invisible")).size()).isEqualTo(0);
				//assertThat(nodeWithFoldIcon.findElements(By.cssSelector("div.node > div.inner-node > div.expand-icon.invisible")).size()).isEqualTo(1);
			}
		});
	}
	
	@Test
	public void selectNodeWithoutFoldIconsTest() {
		runInBrowser(new Callback<TestBrowser>() {
			public void invoke(TestBrowser browser) {
				driver.get(rootURL());

				WebElement rootNode = driver.findElement(By.className("root"));
				WebElement nodeWithoutFoldIcon = rootNode.findElement(By.cssSelector("div.root > div.leftChildren > div.node > div.children > div.node > div.children > div.node"));
				
				//Click on node
				WebElement collapseIcon = nodeWithoutFoldIcon.findElement(By.cssSelector("div.node > div.inner-node > div.collapse-icon > img"));
				
				assertThat(collapseIcon.getCssValue("display")).isEqualTo("none");
				nodeWithoutFoldIcon.click();
				assertThat(collapseIcon.getCssValue("display")).isEqualTo("none");
				
				//Check if Collapse icon is visible
				//assertThat(nodeWithoutFoldIcon.findElements(By.cssSelector("div.node > div.inner-node > div.collapse-icon.invisible")).size()).isEqualTo(1);
				//assertThat(nodeWithoutFoldIcon.findElements(By.cssSelector("div.node > div.inner-node > div.expand-icon.invisible")).size()).isEqualTo(1);
			}
		});
	}
}
