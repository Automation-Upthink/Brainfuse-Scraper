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

    public CalendarPage(WebDriver driver, Calendar today, Calendar endDate) {
        super(driver);
        this.startDate = today.getTime();
        this.endDate = endDate.getTime();

    }

    /**
     * This method extracts the whole calendar for a particular account
     */
    public ArrayList<CalendarObject> extractCalendar() {
        // Wait for the main container to be present
        WebElement mainContainer = waitForPresenceOfElement(By.className("maincontainer"));
        WebElement mainContent = findElement(mainContainer, By.xpath(".//div[contains(@class, 'main-content') and contains(@class, 'column02')]"));
        // Find the main content element
        mainContent = findElement(mainContent, By.id("tdMainContent"));
        // Find the tutoring content element
        WebElement tutoringContent = mainContent.findElement(By.className("tutorsched"));
//        System.out.println("Tutoring content found");
        ArrayList<CalendarObject> events = new ArrayList<>();
        // Loop until 31 days are processed or we reach the end date
//        System.out.println(endDate);
        while (processedDates.size() <= 31 || !processedDates.contains(endDate)) {
            events.addAll(extractSingleCalendarPage(tutoringContent));
            if (!clickNextButton(tutoringContent)) {
                break;
            }
        }
//        System.out.println(events);
//        List<Date> dateList = new ArrayList<>(processedDates);
//        Collections.sort(dateList);
//        System.out.println("Processed Dates: " + dateList);
//        System.out.println("While Loop " + processedDates.size());
        return events;
    }

    /**
     * This method extracts one page of the calendar
     * @param tutoringContent
     * @return
     */
    private ArrayList<CalendarObject> extractSingleCalendarPage(WebElement tutoringContent) {
        // Find the calendar element
        WebElement calendarElement = waitForPresenceOfChildElement(tutoringContent, By.id("calendar"));
        String timezone = timezoneSelect(calendarElement);
//        System.out.println("extract calendar page");
        ArrayList<CalendarObject> array = calendarSchedules(calendarElement, timezone);
//         System.out.println("Single Calendar Page " + array);
        return array;
    }

    private ArrayList<CalendarObject> calendarSchedules(WebElement calendarElement, String timezone){
        ArrayList<CalendarObject> array = new ArrayList<>();
        int maxRetries = 3;
//        System.out.println("calendar schedules");

        boolean elementVisible = false;
        for (int attempt=0; attempt<maxRetries; attempt++) {
            try {
                waitForVisibilityOfElements(By.className("fc-event-container"), 5, null);
                elementVisible = true;
                break;
            } catch (Exception e) {
                logger.warn("Attempt " + (attempt + 1) + " to locate 'fc-event-container' failed.");
            }
        }
        if (!elementVisible) {
            logger.warn("'fc-event-container' not visible after " + maxRetries + " attempts.");
//            System.out.println("Calendar Schedules - Element not visible, So schedules");
            // Get a list of all the dates starting from today for 31 days with timezone in the account timezone and timespan = "-"
            // Generate a list of dates from today for 31 days
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

    private ArrayList<CalendarObject> eachWeek(WebElement parentElement, String timezone) {
        ArrayList <CalendarObject> array = new ArrayList<>();
        List <WebElement> allWeeks = parentElement.findElements(By.xpath("//div[contains(@class, 'fc-row') and contains(@class, 'fc-week')]"));
        for (WebElement week : allWeeks) {
            if (checkFcTodayClassName(week)) {
//                WebElement weekBg = week.findElement(By.className("fc-bg"));
                List<Triple<WebElement, Date, Boolean>> events = checkEventContainer(week);
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


    private List<Triple<WebElement, Date, Boolean>> checkEventContainer(WebElement parentElement) {
        List<Triple<WebElement, Date, Boolean>> eventList = new ArrayList<>();

        WebElement fcSkeleton = parentElement.findElement(By.className("fc-content-skeleton"));
        WebElement fcBg = parentElement.findElement(By.className("fc-bg"));
        List<WebElement> elements = fcBg.findElements(By.tagName("td"));
        WebElement tableElement = fcSkeleton.findElement(By.tagName("table"));
        WebElement tableBodyElement = tableElement.findElement(By.tagName("tbody"));
        WebElement tableRow = tableBodyElement.findElement(By.tagName("tr"));
        List<WebElement> tdElements = tableRow.findElements(By.tagName("td"));
        for (WebElement tdElement : tdElements) {
            int tdIndex = tdElements.indexOf(tdElement);
            WebElement correspondingTr = elements.get(tdIndex);
            String dateString = correspondingTr.getAttribute("data-date");
            Date dayDateObject = null;
            try {
                dayDateObject = new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (tdElement.getAttribute("class").contains("fc-event-container")) {
                // Return a triple object tuple
                eventList.add(new Triple<>(correspondingTr, dayDateObject, true));
            }
            else {
                eventList.add(new Triple<>(correspondingTr, dayDateObject, false));
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
                    Actions actions = new Actions(driver);
                    actions.moveToElement(oneDay).perform();
                    try{
                        WebElement toolTip = waitForVisibilityOfElements(By.id("tooltipTpl"), 2, null);
                        String eventTitle = toolTip.findElement(By.xpath(".//div[contains(@class, 'eventTitle')]//span")).getText();
                        if (eventTitle.equals("On-Call")) {
                            WebElement eventTime = toolTip.findElement(By.className("eventTime"));
                            CalendarObject oneEvent = new CalendarObject(dayDateObject, eventTime.getText(), timezone);
                            array.add(oneEvent);
                        }
                    } catch(Exception e) {
//                        System.out.println("Do nothing " + dayDateObject);
                    }
                } else {
                    CalendarObject noEventObject = new CalendarObject(dayDateObject, "-", timezone);
                    array.add(noEventObject);
                }
            }
        }
        return array;
    }


    private boolean clickNextButton(WebElement tutoringContent) {
        WebElement buttonGroup = tutoringContent.findElement(By.className("fc-button-group"));
        WebElement rightArrow = buttonGroup.findElement(By.className("fc-next-button"));

        for (int attempt = 0; attempt < RETRY_ATTEMPTS; attempt++) {
            try {
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("dlgProgress0")));
                Actions actions = new Actions(driver);
                actions.moveToElement(rightArrow).perform();
                rightArrow.click();
//                System.out.println("Right Arrow clicked");
                return true;
            } catch (ElementClickInterceptedException | TimeoutException e) {
                logger.warn("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == RETRY_ATTEMPTS - 1) {
                    return false;
                }
            }
        }
        return false;
    }

    private String timezoneSelect(WebElement calendarElement) {
        WebElement timezoneElement = findElement(calendarElement, By.className("fc-toolbar"));
        Pattern pattern  = Pattern.compile("\\b(EST|PST|EDT|PDT)\\b");
        String elementText = findElement(timezoneElement, By.className("timezoneSelect")).getText();
        Matcher matcher = pattern.matcher(elementText);
        return matcher.find() ? matcher.group() : null;
    }
}