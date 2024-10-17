package com.example.finandetails;

import static com.example.finandetails.R.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ArrayList<SMSMessage> smsList = new ArrayList<>();
    private ListView listView;
    private Button saveButton;
    private Button logout;
    private static final int READ_SMS_PERMISSION_CODE = 1;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        listView = findViewById(R.id.listView);
        saveButton = findViewById(R.id.save);
        logout = findViewById(R.id.logOut);

        ArrayAdapter<SMSMessage> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        saveButton.setOnClickListener(v -> saveDataToFirebase());

        logout.setOnClickListener(v -> showLogoutConfirmation());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, YourService.class));
            }
            readSms();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(this, YourService.class));
                }
                readSms();
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC");

            if (cursor != null) {
                List<SMSMessage> tempSmsList = new ArrayList<>();
                Pattern bankSenderPattern = Pattern.compile("^[A-Z]{2}-[A-Z0-9]+$", Pattern.CASE_INSENSITIVE); // Regex to match bank-like sender IDs

                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String dateTime = getDateTime(timestampMillis);

                    // Check if the SMS sender matches the pattern of a bank sender ID
                    if (bankSenderPattern.matcher(address).matches()) {
                        List<SMSMessage> transactionDetailsList = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            transactionDetailsList = DataExtraction.extractTransactionDetails(body, address, dateTime, timestampMillis);
                        }
                        if (transactionDetailsList != null && !transactionDetailsList.isEmpty()) {
                            tempSmsList.addAll(transactionDetailsList);
                        }
                    }
                }

                smsList.addAll(tempSmsList);
                Collections.sort(smsList);
                ArrayAdapter<SMSMessage> adapter = (ArrayAdapter<SMSMessage>) listView.getAdapter();
                adapter.notifyDataSetChanged();
            } else {
                Log.e("MainActivity", "Cursor is null, unable to read SMS.");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error reading SMS: " + e.getMessage());
            Toast.makeText(this, "Error reading SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure the cursor is closed in a finally block to prevent resource leaks
            }
        }
    }

    private String getDateTime(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date(milliSeconds));
    }

    private void saveDataToFirebase() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();  // Get the user's unique ID (UID)
        try {
            // Store user's unencrypted information (like email and profession)
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());  // Unencrypted email
            userData.put("profession", "your profession here");  // Unencrypted profession

            // Prepare the SMS data with encryption
            Map<String, Map<String, Object>> smsDataByMonth = new HashMap<>();
            for (SMSMessage sms : smsList) {
                Log.d("SMSDateTime", "Original SMS date-time: " + sms.getTime());

                String encryptedAmount = AESEncryption.encrypt(sms.getAmount());  // Encrypt the transaction amount
                String encryptedType = AESEncryption.encrypt(sms.getType());

                // Create a map to store each SMS's data in a structured manner
                Map<String, Object> singleSmsData = new HashMap<>();
                singleSmsData.put("sender", sms.getSenderId());  // The sender's ID is NOT encrypted
                singleSmsData.put("type", encryptedType);
                singleSmsData.put("dateTime", sms.getTime());  // Original date and time
                singleSmsData.put("amount", encryptedAmount);  // Encrypted transaction amount

                // Extract the month and year from the SMS date to create the key for grouping
                String monthYearKey = extractMonthYear(sms.getTime());
                Log.d("SMSDateTime", "Extracted Month-Year Key: " + monthYearKey);

                // Ensure there's a section for the current month and year
                Map<String, Object> monthData = smsDataByMonth.get(monthYearKey);
                if (monthData == null) {
                    monthData = new HashMap<>();
                    smsDataByMonth.put(monthYearKey, monthData);
                }

                // Generate a unique key for each message based on the date and time (remove / and -)
                String messageKey = generateMessageKey(sms.getTime());

                // Extract the grouping key (3rd to 8th digits) from the messageKey
                String groupingKey = messageKey.substring(2, 8); // Adjust indices if necessary

                // Ensure there's a section for the current grouping key
                Map<String, Object> groupData = (Map<String, Object>) monthData.get(groupingKey);
                if (groupData == null) {
                    groupData = new HashMap<>();
                    monthData.put(groupingKey, groupData);
                }

                // Add the SMS data to the correct grouping node within the month-year section
                groupData.put(messageKey, singleSmsData);
            }

            // Save the grouped SMS data under the user's node
            databaseReference.child("user").child(userId).child("messages").setValue(smsDataByMonth)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to save data", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(MainActivity.this, LogIn.class));
                    finish();
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private String extractMonthYear(String dateTime) {
        // Assume dateTime is in "dd/MM/yyyy HH:mm:ss" format
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

        try {
            Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e("MainActivity", "Date parsing error: " + e.getMessage());
            return "";
        }
    }

    private String generateMessageKey(String dateTime) {
        // Assuming dateTime is in "dd/MM/yyyy HH:mm:ss" format
        return dateTime.replaceAll("[/: ]", ""); // Remove slashes, colons, and spaces to generate a key
    }
}
