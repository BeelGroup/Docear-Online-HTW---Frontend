package helper;

import static org.fest.assertions.Assertions.assertThat;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;

public class Assertions {

	public static int countElements(SearchContext context, By condition){
		return context.findElements(condition).size();
	}
	
	public static void assertElementCountIsNot(SearchContext context, By condition, int count){
		assertThat(countElements(context, condition)).isNotEqualTo(count);
	}
	
	public static void assertElementCountIsLessThan(SearchContext context, By condition, int count){
		assertThat(countElements(context, condition)).isLessThan(count);
	}
	
	public static void assertElementCountIsGreaterThan(SearchContext context, By condition, int count){
		assertThat(countElements(context, condition)).isGreaterThan(count);
	}
	
	public static void assertElementCountEquals(SearchContext context, By condition, int count){
		assertThat(countElements(context, condition)).isEqualTo(count);
	}
	
	public static void assertElementExists(SearchContext context, By condition){
		assertElementCountIsGreaterThan(context, condition, 0);
	}
	
	public static void assertElementExistsOnce(SearchContext context, By condition){
		assertElementCountEquals(context, condition, 1);
	}
	
	public static void assertElementExistsNot(SearchContext context, By condition){
		assertElementCountEquals(context, condition, 0);
	}
}
