package selenium;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import urjanet.pull.web.htmlunit.WebClientfier;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;


/**
 * 
 * @author naveenc
 * 
 */
public class SeleniumUtils {

	private static final SeleniumUtils onlyOne = new SeleniumUtils();
	private final Logger log = LoggerFactory.getLogger(SeleniumUtils.class);

	public static SeleniumUtils getInstance() {

		return onlyOne;
	}

	// singleton class
	private SeleniumUtils() {

	}
	
	/**
	 * It will be used for logging the failure and success message in particular file
	 * 
	 * @param file
	 * @param append
	 * @param message
	 */
	public void logToFile( File file, boolean append, String message ) {

		PrintWriter pwLog = null;
		try {
			pwLog = new PrintWriter(new BufferedWriter(new FileWriter(file, append)));
			pwLog.println(message);
		} catch (IOException e) {
			log.error("(!)Failed while writing to file "+file.getAbsolutePath());
		}
		
		finally {
			IOUtils.closeQuietly(pwLog);
		}
	}
	
	/**
	 * Checks whether the given directory is present or not. If not it will creates the directory and returns the status
	 * 
	 * @param rootDir
	 * @return
	 */
	public boolean createDir(File directory) {
		
		boolean status = false;
		if (!directory.isDirectory() && directory.exists()) {
			directory.delete();
			status = directory.mkdirs();
		} else if( !directory.exists() ) {
			status = directory.mkdirs();
		} else {
			log.debug(directory + "is already exists");
		}
		return status;
	}
	
	/**
	 * @param webDriver
	 * @param SITE_URL
	 * @param username
	 * @param password
	 * @param usernameXpath
	 * @param passwordXpath
	 * @param SubmitXpath
	 * @param logoutXpath
	 * @param isFrame
	 * @return status
	 */
	
	public boolean getLogin(WebDriver webDriver, String SITE_URL,String username, String password, String usernameXpath, String passwordXpath, String SubmitXpath, String logoutXpath, boolean isFrame) {
		
		log.info("Opening log in url: " + SITE_URL);
		webDriver.get(SITE_URL);
		log.info("Wating for login page to get loaded");
		waitForXPaths(webDriver, 40000L, Arrays.asList(usernameXpath), true);
		
		if( isFrame ) {
			if ( !findFrameForGivenXpath(webDriver, usernameXpath) ) {
				log.error("!! Failed to find the frame.");
				return false;
			}
		}
		log.info("Entering Credentials : " + username);
		webDriver.findElement(By.xpath(usernameXpath)).sendKeys(username);
		webDriver.findElement(By.xpath(passwordXpath)).sendKeys(password);

		log.info("Credentials Entered successfully");
		webDriver.findElement(By.xpath(SubmitXpath)).click();
		waitForXPaths(webDriver, 40000L, Arrays.asList(logoutXpath), true);
		
		if ( webDriver.findElements(By.xpath(logoutXpath)).size() > 0 ) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Finding the frame which contains the given xpath.
	 * switch -> switch to particular frame where the xpath is present.
	 * 
	 * @param webDriver
	 * @param xpath
	 * 
	 * @return Return true if webDriver switched to particular frame where the xpath is present, otherwise return false.
	 * 
	 */
	public Boolean findFrameForGivenXpath( WebDriver webDriver, String xpath ) {
		
		return findFrameForGivenXpath(webDriver, xpath, false, null);
	}
	
	/**
	 * Finding the frame which contains the given xpath.
	 * click -> switch to particular frame and perform click.
	 * 
	 * @param webDriver
	 * @param xpath
	 * @param click
	 * 
	 * @return return true if webDriver switched to particular frame and performed click operation, otherwise return false.
	 * 
	 */
	public Boolean findFrameForGivenXpath( WebDriver webDriver, String xpath, Boolean click ) {
		
		return findFrameForGivenXpath(webDriver, xpath, click, null);
	}
	
	/**
	 * Finding the frame which contains the given xpath.
	 * sendKeys -> switch to particular frame and perform sendKeys operation.
	 * 
	 * @param webDriver
	 * @param xpath
	 * @param sendKeys
	 * 
	 * @return return true if webDriver switched to particular frame and performed sendKeys operation, otherwise return false.
	 * 
	 */
	public Boolean findFrameForGivenXpath( WebDriver webDriver, String xpath, String sendKeys ) {
		
		return findFrameForGivenXpath(webDriver, xpath, false, sendKeys);
	}
	
	/**
	 * Finding the frame which contains the given xpath. It performs 3 operations that is 
	 * 1. switch -> switch to particular frame where the xpath is present.
	 * 2. click -> switch to particular frame and perform click.
	 * 3. sendKeys -> switch to particular frame and perform sendKeys operation.
	 * 
	 * @param webDriver
	 * @param xpath
	 * @param click
	 * @param sendKeys
	 * 
	 * @return Return true if any one of three operation performed, otherwise return false.
	 * 
	 */
	private Boolean findFrameForGivenXpath( WebDriver webDriver, String xpath, Boolean click, String sendKeys ) {
		
		List<WebElement> grandFrameElement, parentFrameElement, childFrameElement = null;
		
		waitForXPaths(webDriver, 45000, Arrays.asList("//html | //div | //xhtml"), true);
		webDriver.switchTo().defaultContent();
		
		waitForXPaths(webDriver, 45000, Arrays.asList("//frame | //frameset | //iframe"), true);
		if( (grandFrameElement = webDriver.findElements(By.xpath("//frame | //frameset | //iframe"))).size() > 0 ) {
			
			for (WebElement grandIframe : grandFrameElement) {
				
				webDriver.switchTo().frame(grandIframe);
				waitForTime(1500);
				
				if( webDriver.findElements(By.xpath(xpath)).size() > 0 ) {
					
					log.info(xpath + " Xpath found in " + grandIframe);
					return frameEventHandler(webDriver, xpath, click, sendKeys );
					
				} else if( (parentFrameElement = webDriver.findElements(By.xpath("//frame | //frameset | //iframe"))).size() > 0 ) {
					
					for (WebElement parentIframe : parentFrameElement) {
						
						webDriver.switchTo().frame(parentIframe);
						waitForTime(1500);
						
						if( webDriver.findElements(By.xpath(xpath)).size() > 0 ) {
							
							log.info(xpath + " Xpath found inside the " + parentIframe + " which is under " + grandIframe);
							return frameEventHandler(webDriver, xpath, click, sendKeys );
							
						} else if( (childFrameElement = webDriver.findElements(By.xpath("//frame | //frameset | //iframe"))).size() > 0 ) {
						
							for ( WebElement childIframe : childFrameElement ) {
								
								webDriver.switchTo().frame(childIframe);
								waitForTime(1500);
								
								if( webDriver.findElements(By.xpath(xpath)).size() > 0 ) {
									
									log.info(xpath + " Xpath found inside the " + childIframe + " which is under " + parentIframe);
									return frameEventHandler( webDriver, xpath, click, sendKeys );
								} else {
									log.info(xpath+" xpath found inside this three frame tag -> 'frame' 'frameset' 'iframe'.");
									webDriver.switchTo().defaultContent();
								}
							}
						} else {
							
							webDriver.switchTo().parentFrame();
						}
					}
				} else {
					
					webDriver.switchTo().defaultContent();
				}
			} 
		} else {
			
			waitForXPaths(webDriver, 30000, Arrays.asList(xpath), true);
			if( webDriver.findElements(By.xpath(xpath)).size() > 0 ) {
				
				log.info(xpath + " Xpath found in default window itself.");
				return frameEventHandler(webDriver, xpath, click, sendKeys );
			} else {
				
				log.info("Apologize!! Unable to find the xpath.");
				return false;
			}
			
		}
		return false;
	}
	
	private Boolean frameEventHandler( WebDriver webDriver, String xpath, Boolean click, String sendKeys ) {
		
		if( sendKeys != null) {
			
			webDriver.findElement(By.xpath(xpath)).click();
			webDriver.findElement(By.xpath(xpath)).clear();
			
			webDriver.findElement(By.xpath(xpath)).sendKeys(sendKeys);
			log.info("Webdriver switched to particular frame and value -> '" + sendKeys + "' entered in the xpath -> " + xpath );
		} else if( click ){
			
			javaScriptClick(webDriver, xpath);
			log.info("Webdriver switched to particular frame and clicked the xpath -> " + xpath );
		} else {
			log.info("Webdriver switched to particular frame where the xpath -> " + xpath + " is present.");
		}
	
		return true;
	}
	
	/**
	 * Saves the screen shot in the given file path
	 * 
	 * @param webDriver
	 * @param imageDirectory
	 * @param imageName
	 */
	 public void saveScreenShot(final WebDriver webDriver, File imageDirectory, String imageName)  {
		 
		 File tempFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
		 File imageFile =  new File(imageDirectory, imageName + ".png");
		 FileUtils.deleteQuietly(imageFile);
		 try {
			FileUtils.moveFile(tempFile, imageFile);
		} catch (IOException e) {
			log.error("Failed to save screen shot for " + imageName);
		}
     }
	 
//	private class PartlyDownloadedFile implements FilenameFilter {
//
//		@Override
//		public boolean accept(File file, String name) {
//
//			return (name.endsWith(".part") || name.endsWith(".crdownload" /*
//																		 * for chromium
//																		 */));
//		}
//	}

//	private class DownloadedFile implements FilenameFilter {
//
//		String nameContians = null;
//
//		public DownloadedFile(String nameContains) {
//
//			this.nameContians = nameContains;
//		}
//
////		@Override
////		public boolean accept(File file, String name) {
////
////			return name.contains(nameContians);
////		}
//	}

	/**
	 * Displays summary of the given user
	 * 
	 * @param userName
	 * @param availableBillCount
	 * @param newBillCount
	 * @param skippedCount
	 * @param successCount
	 * @param failedCount
	 */
	public void displayCredentialSummary(String userName, int accountCount, int availableBillCount, int newBillCount, int skippedCount, int successCount, int failedCount) {

		final String TAB = "\t";
		log.info("Credential Summary: " + userName);
		log.info("Total account count in website" + accountCount);
		if (availableBillCount != (skippedCount + newBillCount)) {
			log.error("There is a count miss match in available bill and processed bill");
		}
		log.info("Available bill(s): " + availableBillCount);
		log.info("Already downloaded bill(s): " + skippedCount);
		log.info("New bill(s): " + newBillCount);
		if (newBillCount != 0) {
			log.info(TAB + "Downloaded bill(s): " + successCount);
			log.info(TAB + "Failed: " + failedCount);
		}

	}

	/**
	 * displays account summary
	 * 
	 * @param total
	 * @param skipped
	 * @param success
	 * @param failed
	 */
	public void displayAccountSummary(String accountNumber, int total, List<String> skipped, List<String> success, List<String> failed, boolean history) {

		String TAB = "\t";
		log.info("Account Summary for Account number: " + accountNumber);
		if (!history) {
			log.info("Downloaded recent bill");
		} else {
			log.info("Downloaded history bills");
		}
		log.info("Available bill(s): " + total);

		log.info("Already downloaded bill(s): " + skipped.size());
		for (int j = 0; j < skipped.size(); j++) {
			log.info(TAB + skipped.get(j));
		}

		log.info("Bill(s) available in site: " + (success.size() + failed.size()));

		if ((success.size() + failed.size()) != 0) {

			log.info(TAB + "Downloaded bill(s): " + success.size());
			for (int j = 0; j < success.size(); j++) {
				log.info(TAB + TAB + success.get(j));
			}

			log.info(TAB + "Failed: " + failed.size());
			for (int j = 0; j < failed.size(); j++) {
				log.info(TAB + TAB + failed.get(j));
			}
		}
	}

	/**
	 * Copies Selenium WebDriver cookies to HtmlUnit WebClient cookies
	 * 
	 * @param webDriver
	 * @param htmlUnitWebClient
	 * @return
	 */
	public boolean writeHomePage(String accountNumber, File homePageFile, List<String> savedPagesFileName) {

		// check whether directory is created. If not create one
		if (!homePageFile.getParentFile().isDirectory()) {
			log.debug("Creating home page directory");
			homePageFile.getParentFile().mkdirs();
		}

		if (homePageFile.exists()) {
			log.debug("Removing old home page");
			homePageFile.delete();
		}

		PrintWriter pwHomePage = null;
		try {
			pwHomePage = new PrintWriter(homePageFile);
		} catch (FileNotFoundException e) {
			log.error("Failed creating a home page");
			e.printStackTrace();
			return false;
		}
		pwHomePage.print("<html>");
		pwHomePage.print("<body>");
		pwHomePage.print("<title>" + "Home Page of " + accountNumber + "</title>");
		for (String name : savedPagesFileName) {
			pwHomePage.print("<p>");
			pwHomePage.println(String.format("<a href='%s'>%s</a>", name, name));
			pwHomePage.print("</p>");
		}
		pwHomePage.print("</body>");
		pwHomePage.print("</html>");
		pwHomePage.flush();
		pwHomePage.close();
		return true;
	}

	/**
	 * Copies Selenium WebDriver cookies to HtmlUnit WebClient cookies
	 * 
	 * @param webDriver
	 * @param htmlUnitWebClient
	 * @return
	 */
	public boolean copyCookiesWebDriverToHtmlUnitWebClient(final WebDriver webDriver, WebClient htmlUnitWebClient) {

		CookieManager cookieManager = new CookieManager();
		// clearing cookies
		htmlUnitWebClient.getCookieManager().clearCookies();

		// selects default window
		webDriver.switchTo().window("");
		for (Cookie cookie : webDriver.manage().getCookies()) {

			com.gargoylesoftware.htmlunit.util.Cookie htmlUnitCookie = new com.gargoylesoftware.htmlunit.util.Cookie(cookie.getDomain(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiry(), cookie.isSecure());
			cookieManager.addCookie(htmlUnitCookie);
		}

		htmlUnitWebClient.setCookieManager(cookieManager);
		return true;
	}

	/**
	 * Get WebResponse as input stream. Reads the expected contentType and stores in the given file
	 * 
	 * @param htmlUnitWebClient
	 * @param url
	 * @param file
	 * @param contentType
	 * @return
	 */
	public boolean storeStreamInFile(WebClient htmlUnitWebClient, String url, File file, String contentType) {

		try {
			// Get the input stream
			htmlUnitWebClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			Page page = htmlUnitWebClient.getPage(url);
			if (contentType.contains("pdf") && (!(page.getWebResponse().getContentType().contains("PDF") || page.getWebResponse().getContentType().contains("pdf") || (page.getWebResponse().getContentAsString().startsWith("%PDF-"))))) {
				// if content is pdf extra checks are done the specified content type
				log.error("Unexpected content type " + page.getWebResponse().getContentType() + ". Stopped download");
				return false;
			} else if (!page.getWebResponse().getContentType().contains(contentType)) {
				log.info("Unexpected content type " + page.getWebResponse().getContentType() + ". Stopped download");
				return false;
			}

			byte buffer[] = IOUtils.toByteArray(page.getWebResponse().getContentAsStream());

			OutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				log.error("Failed creating output file " + file.getAbsolutePath());
				e.printStackTrace();
				file.delete();
				return false;
			}
			outputStream.write(buffer);
			outputStream.flush();
			outputStream.close();
		} catch (Exception e) {
			log.error("Failed while reading from InputStream");
			file.delete();
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * get the handle of the new window, which is not in the list and switches to that window
	 * 
	 * @param webDriver
	 * @param existingWindows
	 * @return webDriver with new window as context
	 */
	public String switchToNewWindowHandle(final WebDriver webDriver, List<String> existingWindows) {

		String newHandle = null;
		for (String handle : webDriver.getWindowHandles()) {
			// getting new window handle
			if (!existingWindows.contains(handle)) {
				try {
					newHandle = handle;
					webDriver.switchTo().window(handle);
				} catch (Exception e) {
					log.error("Failed switching to window " + handle);
					break;
				}
			}
		}
		return newHandle;
	}

	/**
	 * Alternative for waitForPageToLoad when using Selenium 2 WebDriver
	 * 
	 * @param webDriver
	 * @param timeOut
//	 */
//	public void waitForPageToLoad(final WebDriver webDriver, String timeOut) {
//
//		String command[] = { timeOut };
//		long start = System.currentTimeMillis();
//		new WaitForPageToLoad().apply(webDriver, command);
//		long duration = System.currentTimeMillis() - start;
//
//		if (duration > Long.valueOf(timeOut)) {
//			throw new SeleniumException("Timed out waiting for action to finish");
//		}
//		/*
//		 * doesn't work for browser above 18.
//		 * ((JavascriptExecutor) webDriver).executeScript("var date = new Date(); var endTime = date.getTime() + arguments[0]; while(endTime > date.getTime() && document.readyState.search(\"complete\") == -1 ); return document.readyState;", 5000).equals("complete");
//		 */
//	}
//
//	/**
//	 * opens a new window with the given window name and url
//	 * 
//	 * @param webDriver
//	 * @param url
//	 * @param windowName
//	 * @return
//	 */
//	public WebDriver openWindow(final WebDriver webDriver, String url, String windowName) {
//
//		String command[] = { url, windowName };
//		new OpenWindow(url, new GetEval(new CompoundMutator(url))).apply(webDriver, command);
//		return webDriver.switchTo().window(windowName);
//	}

	/**
	 * 
	 * @param lastModifiedTime
	 *            - download directories last modified time
	 * @param downloadDir
	 * @param nameContains
	 *            - part file name contains
	 * @return null - no bill downloaded or not null - bill downloaded
	 */
	public File getRecentFile(long lastModifiedTime, File downloadDir, String nameContains, final long DOWNLOAD_WAIT_TIME_LONG) {

		// waiting for bill to download
		File current = null;
		long statusCheckInterval = 10000L;
		long endTime = System.currentTimeMillis() + DOWNLOAD_WAIT_TIME_LONG;
		long fileLastUpdatedTime = lastModifiedTime;
		String TAB = "\t";
		String LINE_FEED = "\n\r";

		log.debug(TAB + "Waiting for download to start");
		while (endTime > System.currentTimeMillis() && current == null) {

			// search for any new .part files added to the directory
//			for (File file : downloadDir.listFiles(onlyOne.new PartlyDownloadedFile())) {
//				if (file.lastModified() > lastModifiedTime) {
//					current = file;
//					break;
//				}
//			}
		}

		if (current != null) {

			log.debug(TAB + "Download started");
			while (current.exists()) {
				// part file exists. Wait for part file to get a little
				// downloaded
				log.debug("Waiting for " + (statusCheckInterval / 1000) + " more seconds for the file to get downloaded");
				endTime += statusCheckInterval;
				while (endTime >= System.currentTimeMillis()) {
					if (!current.exists()) { // checking if the file exists while waiting for next status file size check
						break;
					}
				}

				/*
				 * checking for .part file last modified time. If it is 0, part file removed. (i.e) download completed while waiting for update status if it is same, it is considered to be stopped downloading
				 */
				if (fileLastUpdatedTime == current.lastModified()) {
					log.debug(TAB + "Failed while downloading bill due to network problem.");
					File temp = new File(current.getAbsolutePath().replaceAll(".part|.crdownload", ""));
					log.debug("Removing partly downloaded file(s)" + LINE_FEED + current.getAbsolutePath() + TAB + (temp.exists() ? temp.getAbsolutePath() : ""));
					current.delete();
					if (temp.exists()) {
						temp.delete();
					}
					current = null;
					break;
				} else {
					fileLastUpdatedTime = current.lastModified();
				}
			}
		} else {
			// if file completed downloading before getting a part file
//			for (File file : downloadDir.listFiles(onlyOne.new DownloadedFile(nameContains))) {
//				if (file.lastModified() > lastModifiedTime) {
//					current = file;
//					break;
//				}
//			}
		}

		/*
		 * AFTER SUCCESSFUL DOWNLOAD .part will be removed from the file name So, changing the file description to reflect the name change
		 */
		if (current != null) {
			return new File(current.getAbsolutePath().replaceAll(".part|.crdownload", ""));
		} else {
			return null;
		}
	}

	/**
	 * This waits for given milliseconds to proceed with next line
	 * 
	 * @param timeInMilliSec
	 */
	public void waitForTime(long timeInMilliSec) {

		long waitTime = System.currentTimeMillis() + timeInMilliSec;
		while (waitTime > System.currentTimeMillis())
			;
	}

	/**
	 * waits for a given time and checks for xpath to appear or disappear
	 * 
	 * @param webDriver
	 * @param timeInMilliSec
	 * @param XPathList
	 * @param toAppear
	 *            => true - appear; false - disappear;
	 * @return if any one xpath is found - true or not found - false
	 */
	public boolean waitForXPaths(final WebDriver webDriver, long timeInMilliSec, List<String> XPathList, boolean toAppear) {

		StringBuilder completeXpath = new StringBuilder();

		for (int i = 0; i < XPathList.size(); i++) {

			if (i == 0) {
				// to avoid leading '|' symbol
				completeXpath.append(XPathList.get(i));
			} else {
				completeXpath.append("|").append(XPathList.get(i));
			}
		}

		long waitTime = System.currentTimeMillis() + timeInMilliSec;

		if (toAppear) {
			// appears
			boolean notLoaded = false;
			while (waitTime > System.currentTimeMillis() && (notLoaded = webDriver.findElements(By.xpath(completeXpath.toString())).size() == 0))
				;
			if (!notLoaded) {
				return true;
			} else {
				return false;
			}
		} else {
			// disappears
			boolean notPresent = false;
			while (waitTime > System.currentTimeMillis() && (notPresent = webDriver.findElements(By.xpath(completeXpath.toString())).size() > 0))
				;
			if (!notPresent) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Executes the scripts to Scroll pageup and pagedown
	 * @param webDriver
	 * @param startRange
	 * @param endRange
	 */
	public void javaScriptScroll(final WebDriver webDriver,int startRange,int endRange) { 
		
		JavascriptExecutor executor = (JavascriptExecutor)webDriver;
		executor.executeScript("scroll("+startRange+","+endRange+");");
	}
	
	/**
	 * Executes an script of an element instead of clicking
	 */
	public void javaScriptClick(final WebDriver webDriver , String xpath) {
		WebElement element = webDriver.findElement(By.xpath(xpath));
		JavascriptExecutor executor = (JavascriptExecutor)webDriver;
		executor.executeScript("arguments[0].click();", element);		
	}
	
	/**
	 * prints a HtmlPage source in a file with file's name
	 */
	public boolean savePageSource(WebClient webClient, String url, File file) {

		HtmlPage page = null;
		try {
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			webClient.getOptions().setThrowExceptionOnScriptError(false);

			page = webClient.getPage(url);

		} catch (FailingHttpStatusCodeException e) {
			log.error("Failed with status code: " + e.getStatusCode());
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			log.error("Failed parsing url : [" + url + "]");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			log.error("Failed due to input output exception");
			e.printStackTrace();
			return false;
		}

		// main source file
		if (file.exists()) {
			file.delete();
		}
		// support files dir
		File fileDir = new File(file.getParentFile(), file.getName());
		if (fileDir.exists()) {
			fileDir.delete();
		}

		try {
			page.save(file);
		} catch (IOException e) {
			log.equals("Failed while saving page " + page.getUrl() + " to " + file.getAbsolutePath());
			e.printStackTrace();
			return false;
		}
		log.info("Page saved in " + file.getAbsolutePath());
		return true;
	}

	/**
	 * clicks an element and prints the HtmlPage source in a file with file's name
	 */
	public boolean clickAndSavePageSource(WebClient webClient, String url, File file, String xPath) {

		HtmlPage page = null;
		try {
			webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
			page = webClient.getPage(url);

		} catch (FailingHttpStatusCodeException e) {
			log.error("Failed with status code: " + e.getStatusCode());
			e.printStackTrace();
			return false;
		} catch (MalformedURLException e) {
			log.error("Failed parsing url : [" + url + "]");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			log.error("Failed due to input output exception");
			e.printStackTrace();
			return false;
		}
		log.debug("Clicking before saving the page");
		HtmlElement toClick = page.getFirstByXPath(xPath);
		if (toClick != null) {
			try {
				page = toClick.click();
			} catch (IOException e) {
				log.error("Failed clicking given xpath");
				e.printStackTrace();
				return false;
			}
			onlyOne.waitForTime(2000L);
		}
		// main source file
		if (file.exists()) {
			file.delete();
		}
		// support files dir
		File fileDir = new File(file.getParentFile(), file.getName());
		if (fileDir.exists()) {
			fileDir.delete();
		}

		try {
			page.save(file);
		} catch (IOException e) {
			log.equals("Failed while saving page " + page.getUrl() + " to " + file.getAbsolutePath());
			e.printStackTrace();
			return false;
		}
		log.info("Page saved in " + file.getAbsolutePath());
		return true;
	}

	/*
	 * checks whether the given file is a pdf file
	 */
	public boolean isPdfCorrupted(File file) {

		boolean isPdfCorrupted = true;
		if (null == file) {
			log.debug("File is null");
			return isPdfCorrupted;
		} else if (file.isDirectory()) {
			log.debug("Expected a file but got directory");
			return isPdfCorrupted;
		}
		// *.pdf extension is not checked because the extension may vary for different site

		RandomAccessFile randomAccess = null;
		try {
			randomAccess = new RandomAccessFile(file, "r");
			byte[] content = new byte[10];
			if (-1 != randomAccess.read(content, 0, 10) && new String(content).contains("%PDF-")) { // checking whether the content starts with pdf
				// seek to file end
				randomAccess.seek(randomAccess.length() - 10);
				if (-1 != randomAccess.read(content, 0, 10) && new String(content).contains("%%EOF")) { // checking whether the file ends with %%EOF
					isPdfCorrupted = false;
				}
			}

		} catch (FileNotFoundException e) {
			log.error("Can't open file: " + file.getAbsolutePath());
		} catch (IOException e) {
			log.error("Can't read from file: " + file.getAbsolutePath());
		} finally {
			if (null != randomAccess) {
				try {
					randomAccess.close();
				} catch (IOException e) {
				}
			}
		}

		return isPdfCorrupted;
	}

	/**
	 * Deletes all files and directories in the given directory
	 * 
	 * @return
	 */
	public boolean cleanDir(File directory) {

		log.info("Removing all files in " + directory.getAbsolutePath() + " directory");
		for (File file : directory.listFiles()) {
			if (FileUtils.deleteQuietly(file)) {
				log.info("Deleted file " + file);
			} else {
				log.error("Unable to delete file " + file);
			}
		}
		return directory.listFiles().length == 0 ? false : true;
	}

	/**
	 * Checks and replaces string with regex patten in all htmlFile in directory
	 * 
	 * @param htmlSourceDir
	 * @param regex
	 * @param replaceWith
	 * @return
	 */
//	public boolean replaceInSourceDir(File htmlSourceDir, String regex, String replaceWith) {
//
//		File[] sources = htmlSourceDir.listFiles(new FilenameFilter() {
//
////			@Override
////			public boolean accept(File dir, String name) {
////
////				if (name.endsWith(".html"))
////					return true;
////				else
////					return false;
////			}
//		});

//		for (File source : sources) {
//			replaceInSource(source, regex, replaceWith);
//		}
//		return true;
//	}

	/**
	 * Checks and replaces string with regex patten in htmlFile
	 * 
	 * @param htmlSource
	 * @param regex
	 * @param replaceWith
	 * @return
	 */
	public boolean replaceInSource(File htmlSource, String regex, String replaceWith) {

		Pattern pattern = Pattern.compile(regex);
		Boolean isReplaced = false;
		int matchCount = 0;
		File temp = new File(htmlSource.getAbsoluteFile() + ".temp");

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(htmlSource));
		} catch (FileNotFoundException e) {
			log.error("Failed opening source file: " + htmlSource.getAbsolutePath());
			e.printStackTrace();
		}

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(temp);
		} catch (FileNotFoundException e) {
			log.error("Failed opening new file to write the modified source: " + temp.getAbsolutePath());
			e.printStackTrace();
		}

		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				if (pattern.matcher(line).find()) {
					log.debug("Pattern match line: " + line);
					line = line.replaceAll(regex, "");
					log.debug("Replaced line: " + line);
					isReplaced = true;
					matchCount++;
				}

				pw.write(line);
				pw.flush();
			}
		} catch (IOException e) {
			log.error("Failed while reading source file: " + htmlSource.getAbsolutePath());
			e.printStackTrace();
			try {
				br.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			pw.close();
		}

		try {
			br.close();
		} catch (IOException e) {
			log.error("Failed while closing source file: " + htmlSource.getAbsolutePath());
			e.printStackTrace();
		}
		pw.close();

		log.debug("Matched and replaced: " + matchCount);
		if (matchCount > 0) { // match found replace the original source with updated source
			htmlSource.delete();
			log.info("Replacing modified source " + temp.getAbsolutePath() + ">>" + htmlSource.getAbsolutePath());
			temp.renameTo(htmlSource);
		}
		// remove the temp file
		if (temp.exists())
			temp.delete();

		return isReplaced;
	}
}