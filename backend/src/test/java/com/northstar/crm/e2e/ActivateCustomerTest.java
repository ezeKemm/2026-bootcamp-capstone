package com.northstar.crm.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** CAP-18: an agent moves Ravi Singh from PROSPECT to ACTIVE, and the change is saved. */
class ActivateCustomerTest extends BaseE2ETest {

    private static final String RAVI = "Ravi Singh";
    private static final String RAVI_ID = "7d3b8c73-99f7-4d06-8ce5-f8d8668ed2a1";

    /** Activation is permanent in the local database, so start and finish every run with Ravi as a PROSPECT. */
    @BeforeEach
    void startWithRaviAsProspect() {
        LocalDatabase.setCustomerStatus(RAVI, "PROSPECT");
    }

    @AfterEach
    void leaveRaviAsProspect() {
        LocalDatabase.setCustomerStatus(RAVI, "PROSPECT");
    }

    @Test
    void agentActivatesAProspectAndItIsSaved() {
        // 1. Sign in as an agent and open Ravi Singh (PROSPECT).
        driver.get(BASE_URL + "/customers");
        signIn("agent1", "agent1");
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/customers"));
        openCustomer(RAVI);
        wait.until(ExpectedConditions.urlContains("/customers/" + RAVI_ID));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("customer-name"), RAVI));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".profile-header .status"), "PROSPECT"));

        // Prospects can't have interactions recorded yet: hint shown, no form.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("p.hint")));

        // 2. Activate him.
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.activate"))).click();

        // 3. The page shows the updated customer from the backend: ACTIVE, button gone, record form now available.
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".profile-header .status"), "ACTIVE"));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("button.activate")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-record-interaction-form")));

        // 4. Proof it's in the database: leave, reopen Ravi, and his profile (reloaded from the server) is still ACTIVE.
        driver.navigate().back();
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/customers"));
        openCustomer(RAVI);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".profile-header .status"), "ACTIVE"));
    }
}
