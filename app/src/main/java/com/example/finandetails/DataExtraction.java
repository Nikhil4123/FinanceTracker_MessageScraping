package com.example.finandetails;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtraction {
    private static final Pattern[] TRANSACTION_PATTERNS = {
            // BOI debit pattern
            Pattern.compile("(?i)(Amt\\s+Sent|Rs\\.)\\s+Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+(debited|credited)\\s+(From|To|to)\\s+[\\w\\s]+A/C\\s*\\*?(\\d+)\\s+On\\s+(\\d{2}-\\d{2}|\\d{1,2}[A-Za-z]{3}\\d{2})"),
            // HDFC, ICICI, and SBI patterns can be added similarly
    };

    private static final SimpleDateFormat DEFAULT_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static ArrayList<SMSMessage> extractTransactionDetails(String messageBody, String senderId, String dateTime, long timestampMillis) {
        if (messageBody == null || senderId == null || dateTime == null) {
            throw new IllegalArgumentException("Input parameters must not be null");
        }

        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        for (Pattern pattern : TRANSACTION_PATTERNS) {
            Matcher matcher = pattern.matcher(messageBody);
            if (matcher.find()) {
                String amount = matcher.group(2);
                String transactionType = matcher.group(3).equalsIgnoreCase("debited") ? "Debited" : "Credited";
                String transactionDate = matcher.group(6);
                String formattedDate = convertDateFormat(transactionDate, determineDateFormat(transactionDate));
                detailsList.add(new SMSMessage(senderId, transactionType, amount, formattedDate, dateTime, timestampMillis));
                return detailsList; // Return once a match is found
            }
        }

        return detailsList.isEmpty() ? null : detailsList;
    }

    private static String determineDateFormat(String dateString) {
        if (dateString.matches("\\d{2}-\\d{2}")) {
            return "dd-MM";
        } else if (dateString.matches("\\d{1,2}[A-Za-z]{3}\\d{2}")) {
            return "ddMMMyy";
        }
        return "dd/MM/yyyy"; // Default format
    }

    private static String convertDateFormat(String dateString, String inputFormat) {
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat, Locale.getDefault());
            Date date = inputFormatter.parse(dateString);
            return date != null ? DEFAULT_DATE_FORMATTER.format(date) : dateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Return original date string on error
        }
    }
}
