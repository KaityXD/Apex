package ac.apex.util;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Chat {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String s) {
        if (s == null || s.isEmpty()) return "";
        String working = s;
        try {
            Matcher m = HEX.matcher(working);
            StringBuffer b = new StringBuffer();
            while (m.find()) {
                String hex = m.group(1);
                try {
                    String rep = ChatColor.of("#" + hex).toString();
                    m.appendReplacement(b, rep);
                } catch (Throwable t) {
                    String fallback = hex.equalsIgnoreCase("00f2fe") ? ChatColor.AQUA.toString()
                            : (hex.startsWith("ff") ? ChatColor.RED.toString() : "");
                    m.appendReplacement(b, fallback);
                }
            }
            m.appendTail(b);
            working = b.toString();
        } catch (Throwable ignored) {
        }
        return ChatColor.translateAlternateColorCodes('&', working);
    }

    private Chat() {}
}
