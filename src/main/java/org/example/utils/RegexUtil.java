package org.example.utils;

import com.mifmif.common.regex.Generex;

import java.security.SecureRandom;

public final class RegexUtil {
    private RegexUtil() {
    }

    public static String generatePhoneFromRegex(String regex) {
        String[] regexArr = regex.trim().split("\\|");
        if (regexArr.length == 1) {
            regexArr = regex.trim().split("\\n");
        }

        SecureRandom secureRandom = new SecureRandom();
        int chosenIndex = secureRandom.nextInt(regexArr.length);
        String processedRegex = regexArr[chosenIndex].trim()
                .replaceFirst("^\\^", "")
                .replaceFirst("\\$$", "");

        if (processedRegex.matches("\\d+")) {
            return processedRegex;
        }

        if (processedRegex.contains(".*")) {
            String prefix = processedRegex.replaceAll("\\.\\*", "");
            int remain = prefix.startsWith("0") ? 10 - prefix.length() : 11 - prefix.length();
            remain = Math.max(0, remain);
            processedRegex = prefix + "[0-9]{" + remain + "}";
        } else if (processedRegex.contains("\\d+")) {
            String prefix = processedRegex.replace("\\d+", "");
            int remain = prefix.startsWith("0") ? 10 - prefix.length() : 11 - prefix.length();
            remain = Math.max(0, remain);
            processedRegex = prefix + "[0-9]{" + remain + "}";
        }

        String msisdnFinal;
        try {
            msisdnFinal = new Generex(processedRegex).random();
        } catch (RuntimeException exception) {
            return null;
        }
        return msisdnFinal != null && msisdnFinal.matches("\\d+") ? msisdnFinal : null;
    }
}
