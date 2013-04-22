package features.mindmap.node;

import static org.fest.assertions.Assertions.assertThat;
import static helper.Assertions.*;
import static helper.Wait.*;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import play.libs.F;
import play.test.TestBrowser;
import base.DocearTest;

public class LoginTest extends DocearTest {

    @Test
    public void loginLogoutTest() throws Exception {
        runInBrowser(new F.Callback<TestBrowser>() {
            @Override
            public void invoke(final TestBrowser testBrowser) throws Throwable {      
				gotoRoot();
			
				WebElement loginForm = driver.findElement(By.id("login-form"));
				
				//Fill form
				WebElement loginFormUser = loginForm.findElement(By.id("username"));
				loginFormUser.sendKeys("Alex");
				
				WebElement loginFormPwd = loginForm.findElement(By.id("password"));
				loginFormPwd.sendKeys("secret");
				
				//Send form data and login
				loginForm.findElement(By.tagName("button")).click();
				
				//Wait for page loading
				waitForElement(driver, By.linkText("Logout"));
				
				//Check of logged in
				assertElementExistsOnce(driver, By.linkText("Logout"));
				assertElementExistsNot(driver, By.id("login-form"));
				
				
				//Logout
				WebElement logout = driver.findElement(By.linkText("Logout"));
				logout.click();
				
				//Wait for page loading
				waitForElement(driver, By.id("login-form"));
				
				//Check of logged out
				assertElementExistsNot(driver, By.linkText("Logout"));
				assertElementExistsOnce(driver, By.id("login-form"));
            }
        });
    }
    
    @Test
    public void loginFailTest() throws Exception {
        runInBrowser(new F.Callback<TestBrowser>() {
            @Override
            public void invoke(final TestBrowser testBrowser) throws Throwable {      
				gotoRoot();
				
				WebElement loginForm = driver.findElement(By.id("login-form"));
				
				//Fill form
				WebElement loginFormUser = loginForm.findElement(By.id("username"));
				loginFormUser.sendKeys("Alex");
				
				WebElement loginFormPwd = loginForm.findElement(By.id("password"));
				loginFormPwd.sendKeys("wrongPassword");
				
				//Send form data and login
				loginForm.findElement(By.tagName("button")).click();
				
				//Wait for page loading
				waitForElement(driver, By.id("login-form"), 10);
				
				//Check of logged in
				assertElementExistsNot(driver, By.linkText("Logout"));
				assertElementExistsOnce(driver, By.id("login-form"));
				assertThat(driver.findElement(By.id("login-form")).getText()).contains("The credentials don't match any user.");
            }
        });
    }
}
