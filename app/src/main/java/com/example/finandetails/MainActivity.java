package com.example.finandetails;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ArrayList<SMSMessage> smsList = new ArrayList<>();
    private ListView listView;
    private Button saveButton;
    private Button logout;
    private static final int READ_SMS_PERMISSION_CODE = 1;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

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

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDataToFirebase();
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmation();
            }
        });

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
        Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String dateTime = getDateTime(timestampMillis);

                ArrayList<SMSMessage> transactionDetailsList = null;

                if (isBankSender(address)) {
                    transactionDetailsList = DataExtraction.extractTransactionDetails(body, address, dateTime, timestampMillis);
                }

                if (transactionDetailsList != null) {
                    smsList.addAll(transactionDetailsList);
                }

            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        Collections.sort(smsList);
        ArrayAdapter<SMSMessage> adapter = (ArrayAdapter<SMSMessage>) listView.getAdapter();
        adapter.notifyDataSetChanged();
    }

    private boolean isBankSender(String address) {
        return "JM-BOIIND".equalsIgnoreCase(address) ||
                "VM-BOIIND".equalsIgnoreCase(address) ||
                "JD-BOIIND".equalsIgnoreCase(address) ||
                "AD-HDFCBK".equalsIgnoreCase(address) ||
                "JX-HDFCBK".equalsIgnoreCase(address);
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

            // Now prepare the SMS data with proper encryption for each field
            Map<String, Object> smsData = new HashMap<>();
            for (SMSMessage sms : smsList) {
                // Encrypt specific fields of the SMS data
                //   String encryptedTimestamp = AESEncryption.encrypt(String.valueOf(sms.getTimestamp()));  // Encrypt the timestamp
                String encryptedDate = AESEncryption.encrypt(sms.getTime());  // Encrypt the date
                String encryptedAmount = AESEncryption.encrypt(sms.getAmount());  // Encrypt the transaction amount
                String encryptedType = AESEncryption.encrypt(sms.getType());

                // Create a map to store each SMS's data in a structured manner
                Map<String, Object> singleSmsData = new HashMap<>();
                singleSmsData.put("sender", sms.getSenderId());  // The sender's ID is NOT encrypted
                singleSmsData.put("Type",encryptedType);
                singleSmsData.put("date", encryptedDate);  // Encrypted date
                singleSmsData.put("amount", encryptedAmount);  // Encrypted transaction amount

                // Generate a unique key for each message in the user's "messages" node
                String key = databaseReference.child("user").child(userId).child("messages").push().getKey();

                // Store the structured and encrypted SMS data using the generated key
                smsData.put(key, singleSmsData);
            }

            // Save the user info and the SMS messages under the user's node
            databaseReference.child("user").child(userId).child("messages").setValue(smsData)
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to Log out?")
                .setTitle("Log Out Confirmation")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(MainActivity.this, LogIn.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
