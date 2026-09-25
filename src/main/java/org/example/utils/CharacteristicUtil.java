package org.example.utils;

public final class CharacteristicUtil {
    private CharacteristicUtil() {
    }

    public static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
