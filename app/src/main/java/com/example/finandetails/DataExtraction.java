package com.example.finandetails;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExtraction {

    public static ArrayList<SMSMessage> extractForADHDFCBK(String messageBody, String senderId, String dateTime, long timestampMillis) {
        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        // Regex to extract transaction details for AD-HDFCBK
        String regex = "Amt\\sSent\\sRs\\.(\\d+(\\.\\d{1,2})?)\\s+From\\sHDFC\\sBank\\sA/C\\s\\*(\\d{4})\\s+To\\s([A-Za-z\\s]+)\\s+On\\s(\\d{2}-\\d{2})\\s+Ref\\s(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(messageBody);

        // Extract the details if the pattern is matched
        if (matcher.find()) {
            String amount = matcher.group(1); // Extracted amount (e.g., "10.00")
            String account = matcher.group(3); // Extracted last 4 digits of account number (e.g., "1175")
            String recipient = matcher.group(4).trim(); // Extracted recipient name (e.g., "ANIL RAMASHISH BHAGAT")
            String transactionDate = matcher.group(5); // Extracted transaction date (e.g., "13-09")
            String reference = matcher.group(6); // Extracted reference number (e.g., "425764873414")

            // Format the date if necessary
            String formattedDate = convertDateFormat(transactionDate, "dd-MM", "dd/MM");

            // Add extracted details to SMSMessage list
            SMSMessage smsMessage = new SMSMessage(senderId, "Sent", amount, formattedDate, dateTime, timestampMillis);

            detailsList.add(smsMessage);
        }

        return detailsList.isEmpty() ? null : detailsList;
    }


    public static ArrayList<SMSMessage> extractForJMBoiInd(String messageBody, String senderId, String dateTime, long timestampMillis) {
        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        String regex = "(?i)Rs\\.\\s?(\\d+(\\.\\d{1,2})?)\\s+Credited\\s+to\\syour\\sAc\\s+\\S+\\son\\s(\\d{2}-\\d{2}-\\d{2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(messageBody);

        while (matcher.find()) {
            String amount = matcher.group(1); // Amount
            String transactionDate = matcher.group(2); // Date in dd-MM-yy format

            // Convert the date format to dd/MM/yyyy
            String formattedDate = convertDateFormat(transactionDate, "dd-MM-yy", "dd/MM/yyyy");

            detailsList.add(new SMSMessage(senderId, "Credited", amount, formattedDate, dateTime, timestampMillis));
        }

        return detailsList.isEmpty() ? null : detailsList;
    }

    public static ArrayList<SMSMessage> extractForVMBoiInd(String messageBody, String senderId, String dateTime, long timestampMillis) {
        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        String debitedRegex = "(?i)Rs\\.\\s?(\\d+(\\.\\d{1,2})?)\\s+debited.*?on\\s(\\d{2}[A-Za-z]{3}\\d{2})";
        String creditedRegex = "(?i)Rs\\.\\s?(\\d+(\\.\\d{1,2})?)\\s+credited.*?on\\s(\\d{2}[A-Za-z]{3}\\d{2})";

        Pattern debitedPattern = Pattern.compile(debitedRegex);
        Matcher debitedMatcher = debitedPattern.matcher(messageBody);

        while (debitedMatcher.find()) {
            String amount = debitedMatcher.group(1); // Amount
            String transactionDate = debitedMatcher.group(3); // Date in ddMMMyy format
            String formattedDate = convertDateFormat(transactionDate, "ddMMMyy", "dd/MM/yyyy");
            detailsList.add(new SMSMessage(senderId, "Debited", amount, formattedDate, dateTime, timestampMillis));
        }

        Pattern creditedPattern = Pattern.compile(creditedRegex);
        Matcher creditedMatcher = creditedPattern.matcher(messageBody);

        while (creditedMatcher.find()) {
            String amount = creditedMatcher.group(1); // Amount
            String transactionDate = creditedMatcher.group(3); // Date in ddMMMyy format
            String formattedDate = convertDateFormat(transactionDate, "ddMMMyy", "dd/MM/yyyy");
            detailsList.add(new SMSMessage(senderId, "Credited", amount, formattedDate, dateTime, timestampMillis));
        }

        return detailsList.isEmpty() ? null : detailsList;
    }

    public static ArrayList<SMSMessage> extractForJDBoiInd(String messageBody, String senderId, String dateTime, long timestampMillis) {
        ArrayList<SMSMessage> detailsList = new ArrayList<>();

        // Regex to extract debited and credited details
        String regex = "(?i)Rs\\.\\s?(\\d+(\\.\\d{1,2})?)\\s+(debited|credited).*?on\\s(\\d{2}[A-Za-z]{3}\\d{2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(messageBody);

        while (matcher.find()) {
            String amount = matcher.group(1); // Amount
            String transactionType = matcher.group(3); // Debited or Credited
            String transactionDate = matcher.group(4); // Date in ddMMMyy format
            String formattedDate = convertDateFormat(transactionDate, "ddMMMyy", "dd/MM/yyyy");

            detailsList.add(new SMSMessage(senderId, transactionType.substring(0, 1).toUpperCase() + transactionType.substring(1), amount, formattedDate, dateTime, timestampMillis));
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
            } else {
                return dateString; // Return original if parsing fails
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // Return original if parsing fails
        }
    }
}
