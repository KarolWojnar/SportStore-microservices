package com.shop.notificationservice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantStringsTest {

    @Test
    void constants_shouldHaveCorrectValues() {
        assertEquals("Activate your account", ConstantStrings.ACTIVATION_EMAIL_SUBJECT);
        assertEquals("Reset your password", ConstantStrings.RESET_PASSWORD_SUBJECT);
        assertEquals("Your order delivered!", ConstantStrings.ORDER_SUMMARY_SUBJECT);
        assertEquals("Your order - Sport Store", ConstantStrings.ORDER_EMAIL_SUBJECT);
    }

    @Test
    void emailTemplates_shouldContainPlaceholders() {
        assertTrue(ConstantStrings.ACTIVATION_EMAIL_BODY.contains("%s"));
        assertTrue(ConstantStrings.RESET_PASSWORD_BODY.contains("%s"));
        assertTrue(ConstantStrings.ORDER_SUMMARY_EMAIL_BODY.contains("%s"));
        assertTrue(ConstantStrings.ORDER_EMAIL_BODY.contains("%s"));
    }
}