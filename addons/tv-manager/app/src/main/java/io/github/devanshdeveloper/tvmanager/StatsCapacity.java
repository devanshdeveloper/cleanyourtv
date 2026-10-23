package io.github.devanshdeveloper.tvmanager;

import java.util.Locale;

/**
 * Formatters for memory and storage capacity.
 * Ported from Capacity Stats add-on by lionlev21 (MIT License).
 */
public final class StatsCapacity {
    public static String memory(long availableMb, long totalMb) {
        if (availableMb < 0) return "";
        return Long.toString(availableMb) + (totalMb > 0 ? "/" + totalMb : "");
    }

    public static String storage(long availableBytes, long totalBytes) {
        if (availableBytes < 0) return "";
        double unit = 1024.0 * 1024 * 1024;
        String available = String.format(Locale.US, "%.1f", availableBytes / unit);
        return available + (totalBytes > 0 ? "/" + String.format(Locale.US, "%.1f", totalBytes / unit) : "") + " GB";
    }
}
