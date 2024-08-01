package com.upthink.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.upthink.WebDriverBase;


public class LoginPage extends WebDriverBase{

    final private By usernameField = By.id("login_username");
    final private By passwordField = By.id("login_password");
    final private By loginButton = By.id("btnLogin");

    public LoginPage(WebDriver driver){
        super(driver);
    }

    private void enterUsername(String username) {
        driver.findElement(usernameField).sendKeys(username);
    }

    private void enterPassword(String password) {
        driver.findElement(passwordField).sendKeys(password);
    }

    private void clickLoginButton() {
        driver.findElement(loginButton).click();
    }

    public boolean login(String username, String password) {
        try {
            enterUsername(username);
            enterPassword(password);
            clickLoginButton();
            return isLoginSuccessful();
        } catch (Exception e) {
            logger.error("Login attempt failed", e);
            return false;
        }
    }

    public boolean isLoginSuccessful() {
        return driver.getCurrentUrl().contains("tutorhome.asp");
    }

    
}
