package helper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Wait {

	public static void waitForElement(WebDriver driver, By condition){
		waitForElement(driver, condition, 10);
	}
	
	public static void waitForElement(WebDriver driver, By condition, int seconds){
		WebDriverWait wait = new WebDriverWait(driver, seconds);
		wait.until(ExpectedConditions.presenceOfElementLocated(condition));
	}
}