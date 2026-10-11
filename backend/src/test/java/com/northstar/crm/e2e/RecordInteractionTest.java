package com.northstar.crm.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;


/** Agent story: record an interaction for an ACTIVE customer, and it is saved (survives reloading the timeline). */
class RecordInteractionTest extends BaseE2ETest {

    private static final String AMINA_ID = "5d1c2e0c-3a0d-4b1e-9f9d-8a7d9f4a1c11";

    @Test
    void agentRecordsAnInteractionAndItIsSaved() {
        // Unique text each run, because this test writes real rows to the database.
        String summary = "E2E check " + System.currentTimeMillis();

        // 1. Guest opens the protected customers page -> sent to login -> signs in as an agent -> back on /customers.
        driver.get(BASE_URL + "/customers");
        signIn("agent1", "agent1");
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/customers"));

        // 2. Open Amina Khan (ACTIVE, so agents can record for her).
        openCustomer("Amina Khan");
        wait.until(ExpectedConditions.urlContains("/customers/" + AMINA_ID));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("customer-name"), "Amina Khan"));

        // 3. Fill in and submit the record-interaction form.
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("channel")))).selectByValue("PHONE");
        driver.findElement(By.id("summary")).sendKeys(summary);
        driver.findElement(By.cssSelector("app-record-interaction-form button[type='submit']")).click();

        // 4. The backend saved it: success message + it appears in the timeline.
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("app-record-interaction-form [role='status']"), "Recorded PHONE interaction"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("ol.timeline"), summary));

        // 5. Proof it's in the database: leave the page, come back, and the timeline (reloaded from the server) still has it.
        driver.navigate().back();
        wait.until(ExpectedConditions.urlToBe(BASE_URL + "/customers"));
        openCustomer("Amina Khan");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("ol.timeline"), summary));
    }
}