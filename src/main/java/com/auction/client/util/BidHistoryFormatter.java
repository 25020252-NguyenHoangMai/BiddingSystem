package com.auction.client.util;

import com.auction.dto.BidHistoryEntryDTO;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class BidHistoryFormatter {
    private static final NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);

    public static String format(BidHistoryEntryDTO entry, String currentUsername) {
        String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date(entry.getBidTimeMillis()));

        boolean isMe = Objects.equals(
                        entry.getBidderUsername(),
                        currentUsername
                );

        String name = isMe
                        ? entry.getBidderUsername() + " (you)"
                        : entry.getBidderUsername();

        return String.format(
                "[%s]  %-18s  %s",
                timeStr,
                name,
                fmt.format(entry.getBidAmount())
        );
    }

    public static String formatRealtime(String bidderUsername, double amount, String currentUsername) {
        String timeStr = new SimpleDateFormat("HH:mm:ss").format(new Date());

        boolean isMe = Objects.equals(
                        bidderUsername,
                        currentUsername
                );

        String name = isMe
                        ? bidderUsername + " (you)"
                        : bidderUsername;

        return String.format(
                "[%s]  %-18s  %s",
                timeStr,
                name,
                fmt.format(amount)
        );
    }
}