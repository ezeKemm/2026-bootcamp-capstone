package com.northstar.crm.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guest story: browse names and statuses only; the only action is Sign in. */
class GuestHomePageTest extends BaseE2ETest {

    @Test
    void guestSeesSeededCustomersFromTheDatabase() {
        driver.get(BASE_URL + "/");

        // Wait until the table has rows (the list comes from the backend, so it arrives a moment later).
        List<WebElement> nameCells = wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
                byTestId("home-customer-name"), 0));

        List<String> names = nameCells.stream().map(WebElement::getText).toList();
        assertTrue(names.contains("Amina Khan"), "Expected Amina Khan in " + names);
        assertTrue(names.contains("Ravi Singh"), "Expected Ravi Singh in " + names);
    }

    @Test
    void signInButtonTakesGuestToLoginPage() {
        driver.get(BASE_URL + "/");

        wait.until(ExpectedConditions.visibilityOfElementLocated(byTestId("guest-banner")));
        driver.findElement(byTestId("sign-in-btn")).click();

        wait.until(ExpectedConditions.urlContains("/login"));
        WebElement username = wait.until(
                ExpectedConditions.visibilityOfElementLocated(byTestId("login-username-input")));
        assertTrue(username.isDisplayed());
    }
}
