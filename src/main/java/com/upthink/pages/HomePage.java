package com.upthink.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.upthink.WebDriverBase;

public class HomePage extends WebDriverBase {

    private By navbarLocator = By.id("divTopNavMenu");
    private By scheduleLocator = By.id("webfx-menu-object-10");
    private By calendarLocator = By.id("webfx-menu-object-6"); 

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void navigateToSchedule() {
        if (isNavbarLoaded()) {
            WebElement navbar = findElement(navbarLocator);
            WebElement mySchedule = findElement(navbar, scheduleLocator);

            try {
                hoverAndClick(mySchedule, calendarLocator, 2, 0.5);
            } catch (Exception e) {
                logger.error("Calendar element not found or not visible", e);
            }
        } else {
            logger.error("Failed to navigate to Schedule because the navbar did not load properly.");
        }
    }

    public boolean isNavbarLoaded() {
        try {
            WebElement navbar = waitForPresenceOfElement(navbarLocator, null, null);
            return navbar.isDisplayed();
        } catch (Exception e) {
            logger.error("Navbar did not load properly", e);
            return false;
        }
    }
}
