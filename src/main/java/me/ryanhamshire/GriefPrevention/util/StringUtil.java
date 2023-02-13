package me.ryanhamshire.GriefPrevention.util;

import java.util.Locale;

public class StringUtil {

    public static String enumNameToCamelCase(String value) {
        String[] split = value.split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            if (s.length() > 0) {
                sb.append(Character.toUpperCase(s.charAt(0)));
                if (s.length() > 1) {
                    sb.append(s.substring(1).toLowerCase(Locale.ENGLISH));
                }
            }
        }
        return sb.toString();
    }

}
