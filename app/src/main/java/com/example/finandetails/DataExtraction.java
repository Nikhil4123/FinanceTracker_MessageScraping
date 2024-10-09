package com.example.finandetails;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtraction {
    // Compile the regex patterns once and reuse them for BOI, HDFC, ICICI, and SBI banks
    private static final Pattern[] DEBIT_PATTERNS = {
            // BOI pattern
            Pattern.compile("(?i)Amt\\s+Sent\\s+Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+From\\s+[\\w\\s]+A/C\\s+\\*?(\\d+)\\s+To\\s+([\\w\\s]+)\\s+On\\s+(\\d{2}-\\d{2})"),
            Pattern.compile("(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+debited\\s+A/c(\\w+)\\s+and\\s+credited\\s+to\\s+([\\w\\s]+)\\s+via\\s+UPI\\s+Ref\\s+No\\s+\\d+\\s+on\\s+(\\d{1,2}[A-Za-z]{3}\\d{2})"),

            // HDFC pattern
            Pattern.compile("(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+debited\\s+from\\s+your\\s+A/C\\s+\\*?(\\d+)\\s+for\\s+([\\w\\s]+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})"),

            // ICICI pattern
            Pattern.compile("(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+debited\\s+via\\s+IMPS\\s+from\\s+your\\s+account\\s+\\*?(\\d+)\\s+on\\s+(\\d{1,2}[A-Za-z]{3}\\d{2})"),

            // SBI pattern
            Pattern.compile("(?i)INR\\s+(\\d+(?:\\.\\d{1,2})?)\\s+debited\\s+from\\s+your\\s+SBI\\s+account\\s+\\*?(\\d+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})")
    };

    private static final Pattern[] CREDIT_PATTERNS = {
            // BOI pattern
            Pattern.compile("(?i)Rs\\.\\s*(\\d+(?:\\.\\d{1,2})?)\\s+credited\\s+to\\s+a/c\\s+\\w+(\\d+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})"),
            Pattern.compile("(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+Credited\\s+to\\s+your\\s+Ac\\s+(\\w+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})\\s+by\\s+UPI\\s+ref\\s+No\\.\\d+"),

            // HDFC pattern
            Pattern.compile("(?i)INR\\s+(\\d+(?:\\.\\d{1,2})?)\\s+credited\\s+to\\s+your\\s+HDFC\\s+account\\s+\\*?(\\d+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})"),

            // ICICI pattern
            Pattern.compile("(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+credited\\s+via\\s+NEFT\\s+to\\s+your\\s+ICICI\\s+account\\s+\\*?(\\d+)\\s+on\\s+(\\d{1,2}[A-Za-z]{3}\\d{2})"),

            // SBI pattern
            Pattern.compile("(?i)INR\\s+(\\d+(?:\\.\\d{1,2})?)\\s+credited\\s+to\\s+your\\s+SBI\\s+account\\s+\\*?(\\d+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})")
    };

    private static final SimpleDateFormat DEFAULT_DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static ArrayList<SMSMessage> extractTransactionDetails(String messageBody, String senderId, String dateTime, long timestampMillis) {
        if (messageBody == null || senderId == null || dateTime == null) {
            throw new IllegalArgumentException("Input parameters must not be null");
        }

        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        // Check for debit message formats
        for (Pattern debitPattern : DEBIT_PATTERNS) {
            Matcher debitMatcher = debitPattern.matcher(messageBody);
            if (debitMatcher.find()) {
                String amount = debitMatcher.group(1);
                String transactionDate = debitMatcher.group(debitMatcher.groupCount());
                String formattedDate = convertDateFormat(transactionDate, determineDateFormat(transactionDate));
                detailsList.add(new SMSMessage(senderId, "Debited", amount, formattedDate, dateTime, timestampMillis));
                return detailsList; // Return once a match is found
            }
        }

        // Check for credit message formats
        for (Pattern creditPattern : CREDIT_PATTERNS) {
            Matcher creditMatcher = creditPattern.matcher(messageBody);
            if (creditMatcher.find()) {
                String amount = creditMatcher.group(1);
                String transactionDate = creditMatcher.group(creditMatcher.groupCount());
                String formattedDate = convertDateFormat(transactionDate, determineDateFormat(transactionDate));
                detailsList.add(new SMSMessage(senderId, "Credited", amount, formattedDate, dateTime, timestampMillis));
                return detailsList; // Return once a match is found
            }
        }

        return detailsList.isEmpty() ? null : detailsList;
    }

    private static String determineDateFormat(String dateString) {
        if (dateString.matches("\\d{2}-\\d{2}-\\d{2}")) {
            return "dd-MM-yy";
        } else if (dateString.matches("\\d{2}-\\d{2}")) {
            return "dd-MM";
        } else if (dateString.matches("\\d{1,2}[A-Za-z]{3}\\d{2}")) {
            return "ddMMMyy";
        }
        return "dd/MM/yyyy";
    }

    private static String convertDateFormat(String dateString, String inputFormat) {
        try {
            SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat, Locale.getDefault());
            Date date = inputFormatter.parse(dateString);
            return date != null ? DEFAULT_DATE_FORMATTER.format(date) : dateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString;
        }
    }
}
