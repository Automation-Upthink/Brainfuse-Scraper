package com.upthink;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

import com.upthink.Objects.ProcessedObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.upthink.Objects.CalendarObject;
import com.upthink.pages.CalendarPage;
import com.upthink.pages.HomePage;
import com.upthink.pages.LoginPage;

import io.github.bonigarcia.wdm.WebDriverManager;

public class WebScraperTask implements Callable<List<List<String>>> {

    private final String username;
    private final String password;
    private final String subject;
    private final String singleDual;
    private final String audioCertified;

    public WebScraperTask(String username, String password, String subject, String singleDual, String audioCertified) {
        this.username = username;
        this.password = password;
        this.subject = subject;
        this.singleDual = singleDual;
        this.audioCertified = audioCertified;
    }

    private WebDriver setupDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-extensions",
                "--disable-background-timer-throttling",
                "--disable-backgrounding-occluded-windows",
                "--disable-sync",
                "--disable-translate",
                "--disable-default-apps",
                "--disable-popup-blocking",
                "--disable-blink-features=AutomationControlled"
        );
        return new ChromeDriver(options);
    }

    @Override
    public List<List<String>> call() {
        WebDriver driver = setupDriver();
        List<List<String>> result = new ArrayList<>();

        try {
            LoginPage loginPage = new LoginPage(driver);
            loginPage.navigateTo("https://www.brainfuse.com/login");
            if (loginPage.login(username, password)) {
                System.out.println("Login Successful for " + username + "!");
                result = navigateAndExtractCalendar(driver);
            } else {
                System.out.println("Login Failed for " + username + "!");
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } finally {
            driver.quit();
        }
        return result;
    }

    private List<List<String>> navigateAndExtractCalendar(WebDriver driver) throws ParseException {
        HomePage homePage = new HomePage(driver);
        Calendar today = getStartOfDay();
        Calendar endDate = getEndDate(today);

        homePage.navigateTo("https://www.brainfuse.com/tutor/tutorhome.asp");
        homePage.navigateToSchedule();

        CalendarPage calendarPage = new CalendarPage(driver, today, endDate);
        ArrayList<CalendarObject> calendarObjects =  calendarPage.extractCalendar();
        System.out.println(calendarObjects);
        ProcessedObject processedObject = new ProcessedObject(username, calendarObjects, subject, singleDual, audioCertified);
        return processedObject.processObject();

    }

    private Calendar getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar getEndDate(Calendar startDate) {
        Calendar endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.DAY_OF_MONTH, 31);
        return endDate;
    }
}