package com.upthink;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDriverBase {

    protected WebDriver driver;
    protected int defaultTimeout = 10;
    protected double defaultPollFrequency = 0.5;
    public static final Logger logger = LoggerFactory.getLogger(WebDriverBase.class);

    public WebDriverBase(WebDriver driver) {
        this.driver = driver;
    }

    public void navigateTo(String url) {
        logger.info("Navigating to URL: {}", url);
        driver.get(url);
    }

    public WebElement waitForVisibilityOfElements(By locator) {
        return waitUntil(ExpectedConditions.visibilityOfElementLocated(locator), defaultTimeout, defaultPollFrequency);
    }

    public WebElement waitForVisibilityOfElements(By locator, Integer timeout, Double pollFrequency) {
        return waitUntil(ExpectedConditions.visibilityOfElementLocated(locator), timeout, pollFrequency);
    }

    public WebElement waitForClickabilityOfElement(By locator) {
        return waitUntil(ExpectedConditions.elementToBeClickable(locator), defaultTimeout, defaultPollFrequency);
    }

    public WebElement waitForClickabilityOfElement(By locator, Integer timeout, Double pollFrequency) {
        return waitUntil(ExpectedConditions.elementToBeClickable(locator), timeout, pollFrequency);
    }

    public WebElement waitForPresenceOfElement(By locator) {
        return waitUntil(ExpectedConditions.presenceOfElementLocated(locator), defaultTimeout, defaultPollFrequency);
    }

    public WebElement waitForPresenceOfElement(By locator, Integer timeout, Double pollFrequency) {
        return waitUntil(ExpectedConditions.presenceOfElementLocated(locator), timeout, pollFrequency);
    }

    public WebElement waitForPresenceOfChildElement(WebElement parent, By childLocator) {
        return waitForPresenceOfChildElement(parent, childLocator, defaultTimeout, defaultPollFrequency);
    }

    public WebElement waitForPresenceOfChildElement(WebElement parent, By childLocator, Integer timeout, Double pollFrequency) {
        return waitUntil(ExpectedConditions.presenceOfNestedElementLocatedBy(parent, childLocator), timeout, pollFrequency);
    }



    private WebElement waitUntil(ExpectedCondition<WebElement> condition, Integer timeout, Double pollFrequency) {
        int actualTimeout = (timeout != null) ? timeout : defaultTimeout;
        double actualPollFrequency = (pollFrequency != null) ? pollFrequency : defaultPollFrequency;

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(actualTimeout));
            wait.pollingEvery(Duration.ofMillis((long) (actualPollFrequency * 1000)));
            return wait.until(condition);
        } catch (Exception e) {
            logger.error("An error occurred while waiting for condition: {}", condition, e);
            throw e;
        }
    }

    public void hoverAndClick(WebElement parent, By locator, Integer timeout, Double pollFrequency) {
        Actions actions = new Actions(driver);
        actions.moveToElement(parent).perform(); // Ensure parent is hovered first
//        System.out.println("Hovered over parent element");

        // Wait for the child element to be visible and clickable
        WebElement child = waitUntil(ExpectedConditions.elementToBeClickable(locator), timeout, pollFrequency);
        actions.moveToElement(child).click().perform();
//        System.out.println("Hovered over and clicked on child element");
    }

    public WebElement findElement(By locator) {
        try {
            return driver.findElement(locator);
        } catch (Exception e) {
            logger.error("An error occurred while finding element by locator: {}", locator, e);
            throw e;
        }
    }

    public WebElement findElement(WebElement parent, By locator) {
        try {
            return parent.findElement(locator);
        } catch (Exception e) {
            logger.error("An error occurred while finding element by locator: {} under parent: {}", locator, parent, e);
            throw e;
        }
    }

    public boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isElementVisible(By locator) {
        try {
            WebElement element = driver.findElement(locator);
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Optional: method to configure default timeouts
    public void setDefaultTimeout(int timeout) {
        this.defaultTimeout = timeout;
    }

    public void setDefaultPollFrequency(double pollFrequency) {
        this.defaultPollFrequency = pollFrequency;
    }
}
