package org.example.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexUtilTest {

    @Test
    void generatePhoneFromRegex_plainNumber_returnsUnchangedNumber() {
        assertEquals("0987654321", RegexUtil.generatePhoneFromRegex(" 0987654321 "));
    }

    @Test
    void generatePhoneFromRegex_alternatives_returnsOneAlternative() {
        String result = RegexUtil.generatePhoneFromRegex("0987654321|0912345678");

        assertTrue("0987654321".equals(result) || "0912345678".equals(result));
    }

    @Test
    void generatePhoneFromRegex_newlineAlternatives_returnsOneAlternative() {
        String result = RegexUtil.generatePhoneFromRegex("0987654321\n0912345678");

        assertTrue("0987654321".equals(result) || "0912345678".equals(result));
    }

    @Test
    void generatePhoneFromRegex_dotStar_completesExpectedPhoneLength() {
        String localPhone = RegexUtil.generatePhoneFromRegex("09.*");
        String internationalPhone = RegexUtil.generatePhoneFromRegex("84.*");

        assertNotNull(localPhone);
        assertTrue(localPhone.matches("09\\d{8}"));
        assertNotNull(internationalPhone);
        assertTrue(internationalPhone.matches("84\\d{9}"));
    }

    @Test
    void generatePhoneFromRegex_digitPlus_completesExpectedPhoneLength() {
        String localPhone = RegexUtil.generatePhoneFromRegex("09\\d+");
        String internationalPhone = RegexUtil.generatePhoneFromRegex("84\\d+");

        assertNotNull(localPhone);
        assertTrue(localPhone.matches("09\\d{8}"));
        assertNotNull(internationalPhone);
        assertTrue(internationalPhone.matches("84\\d{9}"));
    }

    @Test
    void generatePhoneFromRegex_boundedRegex_generatesMatchingDigits() {
        String result = RegexUtil.generatePhoneFromRegex("09[1-2]{8}");

        assertNotNull(result);
        assertTrue(result.matches("09[1-2]{8}"));
    }

    @Test
    void generatePhoneFromRegex_nonNumericOrInvalidRegex_returnsNull() {
        assertNull(RegexUtil.generatePhoneFromRegex("[A-Z]+"));
        assertNull(RegexUtil.generatePhoneFromRegex("["));
    }
}
