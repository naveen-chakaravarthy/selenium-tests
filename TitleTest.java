package selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class TitleTest {
	public static void main(String []a) {
		//initialization
//		System.setProperty("webdriver.gecko.driver", "/Users/naveenchakar/Downloads/geckodriver");
		WebDriver firefox = new HtmlUnitDriver();
		//test-1
		firefox.get("https://www.amazon.in/");
		String title = "Shop Online";
		System.out.println("Current page Title: " + firefox.getTitle());
		if(firefox.getTitle().contains(title)) {
			System.out.println("Test case passed...");
		} else {
			System.out.println("Test Failed...");
		}
		
		System.out.println(firefox.findElements(By.id("courses")).size());
		//termination
		firefox.close();
	}
}
