package com.upthink.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.upthink.WebDriverBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPage extends WebDriverBase {
    private static Logger log = LoggerFactory.getLogger(LoginPage.class);

    private final By usernameField = By.id("login_username");
    private final By passwordField = By.id("login_password");
    private final By loginButton = By.id("btnLogin");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    private void enterUsername(String username) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement usernameElement = wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField));
        usernameElement.clear();
        usernameElement.sendKeys(username);
    }

    private void enterPassword(String password) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement passwordElement = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
        passwordElement.clear();
        passwordElement.sendKeys(password);
    }

    private void clickLoginButton() {
        log.info("Waiting for login button to be clickable");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(loginButton));
        button.click();
        log.info("Clicked login button");
    }

    public boolean login(String username, String password) {
        int maxAttempts = 3;
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                // Always re-fetch and clear elements before each attempt.
                enterUsername(username);
                enterPassword(password);
                clickLoginButton();

                // Wait for either a successful login URL or a login error indicator.
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                wait.until(ExpectedConditions.or(
                        ExpectedConditions.urlContains("tutorhome.asp"),
                        ExpectedConditions.presenceOfElementLocated(By.id("login_error"))
                ));

                if (isLoginSuccessful()) {
                    log.info("Login attempt {} for {} - landed on URL {}", attempts+1, username, driver.getCurrentUrl());
                    return true;
                } else {
                    log.info("Login attempt {} failed. Retrying...", (attempts + 1));
                }
            } catch (StaleElementReferenceException e) {
                log.warn("Login attempt {} encountered a stale element. Retrying...", (attempts + 1));
            } catch (Exception e) {
                log.warn("Login attempt {} encountered an exception: {}", (attempts + 1), e.getMessage());
            }
            attempts++;
            try { Thread.sleep(2000); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        logger.error("All {} login attempts exhausted for {}. Final URL: {}",
                maxAttempts, username, driver.getCurrentUrl());
        return false;
    }

    public boolean isLoginSuccessful() {
        return driver.getCurrentUrl().contains("tutorhome.asp");
    }
}


















//package com.upthink.pages;
//
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//
//import com.upthink.WebDriverBase;
//
//
//public class LoginPage extends WebDriverBase{
//
//    final private By usernameField = By.id("login_username");
//    final private By passwordField = By.id("login_password");
//    final private By loginButton = By.id("btnLogin");
//
//    public LoginPage(WebDriver driver){
//        super(driver);
//    }
//
//    private void enterUsername(String username) {
//        driver.findElement(usernameField).sendKeys(username);
//    }
//
//    private void enterPassword(String password) {
//        driver.findElement(passwordField).sendKeys(password);
//    }
//
//    private void clickLoginButton() {
//        driver.findElement(loginButton).click();
//    }
//
//    public boolean login(String username, String password) {
//        int maxAttempts = 3;
//        int attempts = 0;
//        while (attempts < maxAttempts) {
//            try {
//                enterUsername(username);
//                enterPassword(password);
//                clickLoginButton();
//                if (isLoginSuccessful()) {
//                    return true;
//                } else {
//                    System.out.print("Login attempt " + (attempts + 1) + " failed. Retrying...");
//                }
//            } catch (Exception e) {
//                System.out.print("Login attempt " + (attempts + 1) + " encountered an exception ");
//            }
//            attempts++;
//        }
//        return false;
//    }
//
//
//    public boolean isLoginSuccessful() {
//        return driver.getCurrentUrl().contains("tutorhome.asp");
//    }
//
//
//}
