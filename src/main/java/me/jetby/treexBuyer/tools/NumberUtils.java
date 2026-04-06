package me.jetby.treexBuyer.tools;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Locale;

public class NumberUtils {
    private static final DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
    private static final DecimalFormat dfPlain = (DecimalFormat) DecimalFormat.getInstance(Locale.US);

    static {
        df.applyPattern("#,##0.##");
        dfPlain.applyPattern("0.##");
    }

    public static String formatWithCommas(long value) {
        return df.format(value);
    }

    public static String formatWithCommas(String value) {
        return df.format(Double.parseDouble(value));
    }

    public static String formatWithCommas(BigInteger value) {
        return df.format(value);
    }

    public static String formatWithCommas(double value) {
        return df.format(value);
    }

    public static String format(double value) {
        return dfPlain.format(value);
    }

    public static String format(long value) {
        return dfPlain.format(value);
    }
}