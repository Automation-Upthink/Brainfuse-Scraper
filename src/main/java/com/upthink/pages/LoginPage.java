package com.upthink.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.upthink.WebDriverBase;

public class LoginPage extends WebDriverBase {

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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(loginButton));
        button.click();
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
                    return true;
                } else {
                    System.out.println("Login attempt " + (attempts + 1) + " failed. Retrying...");
                }
            } catch (StaleElementReferenceException e) {
                System.out.println("Login attempt " + (attempts + 1) + " encountered a stale element. Retrying...");
            } catch (Exception e) {
                System.out.println("Login attempt " + (attempts + 1) + " encountered an exception: " + e.getMessage());
            }
            attempts++;
        }
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
