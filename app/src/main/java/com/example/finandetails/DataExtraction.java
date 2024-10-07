package com.example.finandetails;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtraction {
    public static ArrayList<SMSMessage> extractTransactionDetails(String messageBody, String senderId, String dateTime, long timestampMillis) {
        if (messageBody == null || senderId == null || dateTime == null) {
            throw new IllegalArgumentException("Input parameters must not be null");
        }

        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        // Patterns for messages
        String debitRegex = "(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+debited\\s+A/c(\\w+)\\s+and\\s+credited\\s+to\\s+([\\w\\s]+)\\s+via\\s+UPI\\s+Ref\\s+No\\s+\\d+\\s+on\\s+(\\d{1,2}[A-Za-z]{3}\\d{2})";
        String creditRegex = "(?i)Rs\\.(\\d+(?:\\.\\d{1,2})?)\\s+Credited\\s+to\\s+your\\s+Ac\\s+(\\w+)\\s+on\\s+(\\d{2}-\\d{2}-\\d{2})\\s+by\\s+UPI\\s+ref\\s+No\\.\\d+";

        // Check for debited message format
        Pattern debitPattern = Pattern.compile(debitRegex);
        Matcher debitMatcher = debitPattern.matcher(messageBody);

        if (debitMatcher.find()) {
            String amount = debitMatcher.group(1);
            String accountNumber = debitMatcher.group(2);
            String creditedTo = debitMatcher.group(3);
            String transactionDate = debitMatcher.group(4);
            String formattedDate = convertDateFormat(transactionDate, "ddMMMyy", "dd/MM/yyyy");

            detailsList.add(new SMSMessage(senderId, "Debited", amount, formattedDate, dateTime, timestampMillis));
        } else {
            // Check for credited message format
            Pattern creditPattern = Pattern.compile(creditRegex);
            Matcher creditMatcher = creditPattern.matcher(messageBody);

            if (creditMatcher.find()) {
                String amount = creditMatcher.group(1);
                String accountNumber = creditMatcher.group(2);
                String transactionDate = creditMatcher.group(3);
                String formattedDate = convertDateFormat(transactionDate, "dd-MM-yy", "dd/MM/yyyy");

                detailsList.add(new SMSMessage(senderId, "Credited", amount, formattedDate, dateTime, timestampMillis));
            } else {
                System.out.println("No matches found for either pattern.");
            }
        }

        return detailsList.isEmpty() ? null : detailsList;
    }

    private static String convertDateFormat(String dateString, String inputFormat, String outputFormat) {
        SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat, Locale.getDefault());
        SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat, Locale.getDefault());
        try {
            Date date = inputFormatter.parse(dateString);
            if (date != null) {
                return outputFormatter.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateString; // Return original if parsing fails
    }
}
