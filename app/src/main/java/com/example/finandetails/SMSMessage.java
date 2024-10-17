package com.example.finandetails;

public class SMSMessage implements Comparable<SMSMessage> {
    private String senderId;
    private String type;
    private String amount;
    private String date;
    private String time;
    private long timestamp;
    public SMSMessage() {
    }
    public SMSMessage(String senderId, String type, String amount, String date, String time, long timestamp) {
        this.senderId = senderId;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.timestamp = timestamp;
    }
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTimestamp() {
        return timestamp;
    }


    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public String toString() {
        return "Sender: " + senderId + "\nType: " + type + "\nAmount: " + amount +  "\nDate_Time: " + time + "\n\n";
    }

    @Override
    public int compareTo(SMSMessage other) {
        return Long.compare(this.timestamp, other.timestamp);
    }
}