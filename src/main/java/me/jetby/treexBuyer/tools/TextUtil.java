package me.jetby.treexBuyer.tools;

import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextUtil {

    private final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
    private final Pattern GRADIENT_TAG = Pattern.compile("<(/?)#([a-fA-F0-9]{6})>");
    private final char COLOR_CHAR = '§';

    public String setPapi(Player player, String text) {
        if (text == null) return null;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public List<String> setPapi(Player player, List<String> text) {
        if (text == null) return null;
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public List<String> colorize( @Nullable List<String> list) {

        List<String> strings = new ArrayList<>();
        if (list==null) return strings;
        for (String string : list) {
            strings.add(colorize(string));
        }
        return strings;
    }

    public String colorize(@Nullable String message) {
        if (message==null) return "";
        message = processGradients(message);

        final Matcher matcher = HEX_PATTERN.matcher(message);
        final StringBuilder builder = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + group.charAt(0) +
                            COLOR_CHAR + group.charAt(1) +
                            COLOR_CHAR + group.charAt(2) +
                            COLOR_CHAR + group.charAt(3) +
                            COLOR_CHAR + group.charAt(4) +
                            COLOR_CHAR + group.charAt(5));
        }
        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    private String processGradients(String message) {
        Matcher matcher = GRADIENT_TAG.matcher(message);
        StringBuilder output = new StringBuilder();
        int last = 0;
        String startHex = null;
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String before = message.substring(last, matcher.start());
            if (startHex == null) {
                output.append(before);
            } else {
                buffer.append(before);
            }
            String slash = matcher.group(1);
            String hex = matcher.group(2);
            if (startHex == null) {
                if (slash.isEmpty()) {
                    startHex = hex;
                    buffer.setLength(0);
                } else {
                    output.append(matcher.group(0));
                }
            } else {
                String colored = applyGradient(buffer.toString(), startHex, hex);
                output.append(colored);
                if (slash.isEmpty()) {
                    startHex = hex;
                    buffer.setLength(0);
                } else {
                    startHex = null;
                }
            }
            last = matcher.end();
        }
        String tail = message.substring(last);
        if (startHex == null) {
            output.append(tail);
        } else {
            buffer.append(tail);
            String colored = applyGradient(buffer.toString(), startHex, startHex);
            output.append(colored);
        }
        return output.toString();
    }

    private String applyGradient(String text, String hex1, String hex2) {
        if (text.isEmpty()) {
            return "";
        }
        int r1 = Integer.parseInt(hex1.substring(0, 2), 16);
        int g1 = Integer.parseInt(hex1.substring(2, 4), 16);
        int b1 = Integer.parseInt(hex1.substring(4, 6), 16);
        int r2 = Integer.parseInt(hex2.substring(0, 2), 16);
        int g2 = Integer.parseInt(hex2.substring(2, 4), 16);
        int b2 = Integer.parseInt(hex2.substring(4, 6), 16);
        int visible = 0;
        int pos = 0;
        int len = text.length();
        while (pos < len) {
            if (text.charAt(pos) == '&' && pos + 1 < len && isValidColorCharacter(text.charAt(pos + 1))) {
                pos += 2;
            } else {
                visible++;
                pos++;
            }
        }
        StringBuilder segment = new StringBuilder();
        Set<Character> activeFormats = new HashSet<>();
        int v = 0;
        pos = 0;
        while (pos < len) {
            char c = text.charAt(pos);
            if (c == '&' && pos + 1 < len && isValidColorCharacter(text.charAt(pos + 1))) {
                char codeChar = text.charAt(pos + 1);
                char code = Character.toLowerCase(codeChar);
                segment.append('&').append(codeChar);
                if (code == 'r') {
                    activeFormats.clear();
                } else if (isFormat(code)) {
                    activeFormats.add(code);
                } else {
                    activeFormats.clear();
                }
                pos += 2;
            } else {
                if (visible > 0) {
                    double ratio = (visible == 1) ? 0 : (double) v / (visible - 1);
                    int r = (int) (r1 + ratio * (r2 - r1));
                    int g = (int) (g1 + ratio * (g2 - g1));
                    int b = (int) (b1 + ratio * (b2 - b1));
                    String hex = String.format("%02x%02x%02x", r, g, b);
                    segment.append("&#").append(hex);
                    for (char f : activeFormats) {
                        segment.append('&').append(f);
                    }
                }
                segment.append(c);
                v++;
                pos++;
            }
        }
        return segment.toString();
    }

    private boolean isFormat(char c) {
        return c == 'k' || c == 'l' || c == 'm' || c == 'n' || c == 'o';
    }

    public String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();

        for (int i = 0, length = b.length - 1; i < length; ++i) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private boolean isValidColorCharacter(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                c == 'r' ||
                (c >= 'k' && c <= 'o') ||
                c == 'x' ||
                (c >= 'A' && c <= 'F') ||
                c == 'R' ||
                (c >= 'K' && c <= 'O') ||
                c == 'X';
    }
}