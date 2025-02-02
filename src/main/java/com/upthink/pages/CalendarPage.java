package com.upthink.pages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.upthink.Objects.CalendarObject;
import com.upthink.Objects.Triple;
import com.upthink.WebDriverBase;

public class CalendarPage extends WebDriverBase{

    private int counter = 0;
    private int noScheduleDays = 0;
    private Set<Date> processedDates = new HashSet<>();
    private Date startDate;
    private Date endDate;
    private int previousEventId;
    private int accountNotScheduled = 0;
    private static final int RETRY_ATTEMPTS = 3;
    private String accountName;

    public CalendarPage(WebDriver driver, Calendar today, Calendar endDate, String accountName) {
        super(driver);
        this.startDate = today.getTime();
        this.endDate = endDate.getTime();
        this.accountName = accountName;
    }


    /**
     * Retry logic for finding an element using a dynamic function.
     *
     * @param function    The function to execute for finding the element.
     * @param parent      The parent element to pass to the function (can be null if not needed).
     * @param maxRetries  Maximum number of retries.
     * @param retryDelay  Delay between retries in milliseconds.
     * @return WebElement if found, null if not found after retries.
     */
    private WebElement retryFindElement(Function<WebElement, WebElement> function, WebElement parent, int maxRetries, int retryDelay) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                WebElement element = function.apply(parent);
                if (element != null) {
                    return element;
                }
            } catch (NoSuchElementException | TimeoutException e) {
                System.out.println("Attempt " + (i + 1) + " to locate 'tutorsched' failed for account " + accountName);
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();  // Restore interrupted status
                }
            }
        }
        return null;  // Return null if not found after retries
    }


    /**
     * This method extracts the whole calendar for a particular account
     */
    public ArrayList<CalendarObject> extractCalendar() throws ParseException {
        // Wait for the main container to be present
        WebElement mainContainer = waitForPresenceOfElement(By.className("maincontainer"));
        WebElement mainContent = findElement(mainContainer, By.xpath(".//div[contains(@class, 'main-content') and contains(@class, 'column02')]"));
        // Find the main content element
        WebElement tdMainContent = findElement(mainContent, By.id("tdMainContent"));
        // Find the tutoring content element
        WebElement tutoringContent = retryFindElement(
                parent -> waitForPresenceOfChildElement(parent, By.xpath(".//div[contains(@class, 'tutorsched')]"), 30, 100.0),
                tdMainContent,5, 1000
        );

        // If tutoring content still not found after retries, handle it (throw an exception or return empty)
        if (tutoringContent == null) {
            System.out.println("Account "+ accountName + " has no tutorsched");
            throw new NoSuchElementException("Could not find the 'tutorsched' element after multiple retries.");
        }

        ArrayList<CalendarObject> events = new ArrayList<>();
        while (processedDates.size() <= 31 && !processedDates.contains(endDate)) {
            events.addAll(extractSingleCalendarPage(tutoringContent));
            if (!clickNextButton(tutoringContent)) {
                break;
            }
//            waitForNewFcEventContainerToLoad();
        }
        return events;
    }


    private void waitForNewPageToLoad(WebElement tutoringContent) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // Wait for any loading progress to disappear (adjust this based on your page's behavior)
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("dlgProgress0")));
        // Wait for new content to load (use any unique identifier that changes between pages, e.g., a day element)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("fc-day-grid")));  // Adjust the locator if necessary
    }


    /**
     * This method extracts one page of the calendar
     * @param tutoringContent
     * @return
     */
    private ArrayList<CalendarObject> extractSingleCalendarPage(WebElement tutoringContent) throws ParseException {
        // Find the calendar element
        WebElement calendarElement = waitForPresenceOfChildElement(tutoringContent, By.id("calendar"));
        String timezone = timezoneSelect(calendarElement);
        ArrayList<CalendarObject> array = calendarSchedules(calendarElement, timezone);
        System.out.println("Array size" + array.size() + " account " + accountName);
        return array;
    }

    private ArrayList<CalendarObject> calendarSchedules(WebElement calendarElement, String timezone) throws ParseException {
        ArrayList<CalendarObject> array = new ArrayList<>();
        int maxRetries = 3;

        boolean elementVisible = false;
        for (int attempt=0; attempt<maxRetries; attempt++) {
            try {
                waitForVisibilityOfElements(By.className("fc-event-container"), 30, 10.0);
                elementVisible = true;
                break;
            } catch (Exception e) {
                System.out.println("Attempt " + (attempt + 1) + " to locate 'fc-event-container' failed. " + accountName);
            }
        }
        if (!elementVisible) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);

            while (calendar.getTime().compareTo(endDate) <= 0 && processedDates.size() <= 31) {
                Date currentDate = calendar.getTime();
                // Only add the date if it's within the start and end date range and not already processed
                if (!processedDates.contains(currentDate)) {
                    processedDates.add(currentDate);
                    // Create a CalendarObject for the date with no event
                    CalendarObject noEventObject = new CalendarObject(currentDate, "-", timezone);
                    array.add(noEventObject);
                }
                // Move to the next day
                calendar.add(Calendar.DATE, 1);
            }
            return array;
        }
        WebElement calendarView = findElement(calendarElement, By.className("fc-view-container"));
        WebElement calendarBody = findElement(calendarView, By.className("fc-body"));
        WebElement dayGrid = findElement(calendarBody, By.className("fc-day-grid"));
        ArrayList<CalendarObject> partArray = eachWeek(dayGrid, timezone);
        array.addAll(partArray);
        return array;
    }

    private ArrayList<CalendarObject> eachWeek(WebElement parentElement, String timezone) throws ParseException {
        ArrayList <CalendarObject> array = new ArrayList<>();
        List <WebElement> allWeeks = parentElement.findElements(By.xpath("//div[contains(@class, 'fc-row') and contains(@class, 'fc-week')]"));
        for (WebElement week : allWeeks) {
            if (checkFcTodayClassName(week)) {
                WebElement weekSkeleton = week.findElement(By.className("fc-content-skeleton"));
                List<Triple<WebElement, Date, Boolean>> events = checkEventContainer(weekSkeleton);
                ArrayList<CalendarObject> calendarObjects = eachDay(events, timezone);
                array.addAll(calendarObjects);
            }
        }
        return array;
    }

    private Boolean checkFcTodayClassName(WebElement parentElement) {
        try{
            parentElement.findElement(By.xpath(".//td[contains(@class, 'fc-today') or contains(@class, 'fc-future')]"));
                return true;
        } catch (NoSuchElementException e) {
            return false;
        }

    }


    private List<Triple<WebElement, Date, Boolean>> checkEventContainer(WebElement parentElement) throws ParseException {
        List<Triple<WebElement, Date, Boolean>> eventList = new ArrayList<>();
        WebElement table = parentElement.findElement(By.tagName("table"));
        WebElement tHead = table.findElement(By.tagName("thead"));
        List<WebElement> dates = tHead.findElements(By.tagName("td"));
        WebElement tBody = table.findElement(By.tagName("tbody"));
        WebElement tableRow = tBody.findElement(By.tagName("tr"));
        List<WebElement> tdElements = tableRow.findElements(By.tagName("td"));

        for (WebElement tdElement : tdElements) {
            int tdIndex = tdElements.indexOf(tdElement);
            WebElement dateWebElement = dates.get(tdIndex);
            String dateString = dateWebElement.getAttribute("data-date");
            Date dayDateObject = null;
            try {
                dayDateObject = new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (tdElement.getAttribute("class").contains("fc-event-container")) {
                // Return a triple object tuple
                eventList.add(new Triple<>(tdElement, dayDateObject, true));
            }
            else {
                eventList.add(new Triple<>(tdElement, dayDateObject, false));
            }
        }

        return eventList;
    }

    private ArrayList<CalendarObject> eachDay(List<Triple<WebElement, Date, Boolean>> events, String timezone) {
        ArrayList<CalendarObject> array = new ArrayList<>();
        for(Triple<WebElement, Date, Boolean> event : events) {
            WebElement oneDay = event.getKey();
            Date dayDateObject = event.getValue();
            if (dayDateObject != null && dayDateObject.compareTo(endDate) <= 0
                    && dayDateObject.compareTo(startDate) >= 0
                    && !processedDates.contains(dayDateObject)) {
                processedDates.add(dayDateObject);
                if (event.getBool() == true) {
                    try{
                        Actions actions = new Actions(driver);
                        actions.moveToElement(oneDay).pause(Duration.ofSeconds(1)).build().perform();
                        WebElement tooltipDivClass = waitForVisibilityOfElements(By.className("tooltip"), 2, null);
                        WebElement tooltipTpl = tooltipDivClass.findElement(By.id("tooltipTpl"));
                        String eventTitle = tooltipTpl.findElement(By.xpath(".//div[contains(@class, 'eventTitle')]//span")).getText();
                        if (eventTitle.equals("On-Call")) {
                            WebElement eventTime = tooltipTpl.findElement(By.className("eventTime"));
                            CalendarObject oneEvent = new CalendarObject(dayDateObject, eventTime.getText(), timezone);
                            array.add(oneEvent);
                        }
                    } catch(Exception e) {
                        System.out.println("Do nothing " + dayDateObject);
                    }
                }
                else {
                    CalendarObject noEventObject = new CalendarObject(dayDateObject, "-", timezone);
                    array.add(noEventObject);
                }
//                if (!processedDates.contains(dayDateObject)) {
//                    processedDates.add(dayDateObject);
//
//                    // If there are no events, add a placeholder
//                    if (!event.getBool()) {
//                        CalendarObject noEventObject = new CalendarObject(dayDateObject, "-", timezone);
//                        array.add(noEventObject);
//                    }
//                }
            }
        }
        return array;
    }


    private boolean clickNextButton(WebElement tutoringContent) {
        WebElement buttonGroup = tutoringContent.findElement(By.className("fc-button-group"));
        WebElement rightArrow = buttonGroup.findElement(By.className("fc-next-button"));

        for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("dlgProgress0")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", rightArrow);

                System.out.println("Right arrow clicked");
                return true;
            } catch (ElementClickInterceptedException | TimeoutException e) {
                System.out.println("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == RETRY_ATTEMPTS - 1) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Waits for at least one fresh fc-event-container to be present/visible
     * after the page refreshes or updates.
     */
    private void waitForNewFcEventContainerToLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("fc-event-container")));
    }


    private String timezoneSelect(WebElement calendarElement) {
        WebElement timezoneElement = findElement(calendarElement, By.className("fc-toolbar"));
        Pattern pattern  = Pattern.compile("\\b(MST|EST|PST|EDT|PDT|CST|HST)\\b");
        String elementText = findElement(timezoneElement, By.className("timezoneSelect")).getText();
        Matcher matcher = pattern.matcher(elementText);
        return matcher.find() ? matcher.group() : "-";
    }


}