package com.example.finandetails;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    private  Button logout;
    private static final int READ_SMS_PERMISSION_CODE = 1;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        auth = FirebaseAuth.getInstance();
        listView = findViewById(R.id.listView);
        saveButton = findViewById(R.id.save);
        logout= findViewById(R.id.logOut);

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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Are you sure you want to  Log out?")
                        .setTitle("Log Out Confirmation")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked Yes button
                                auth.signOut();

                                Intent intent = new Intent(MainActivity.this, LogIn.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                                dialog.dismiss(); // Dismiss the dialog if user clicks No
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            readSms();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms();
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long timestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                String dateTime = getDateTime(timestampMillis);

                // Filter and process messages from specific senders
                ArrayList<SMSMessage> transactionDetailsList = null;
                if ("JM-BOIIND".equalsIgnoreCase(address)) {
                    transactionDetailsList = DataExtraction.extractForJMBoiInd(body, address, dateTime, timestampMillis);
                } else if ("VM-BOIIND".equalsIgnoreCase(address)) {
                    transactionDetailsList = DataExtraction.extractForVMBoiInd(body, address, dateTime, timestampMillis);
                } else if ("JD-BOIIND".equalsIgnoreCase(address)) {
                    transactionDetailsList = DataExtraction.extractForJDBoiInd(body, address, dateTime, timestampMillis);
                }
                else if ("AD-HDFCBK".equalsIgnoreCase(address)) {
                    transactionDetailsList = DataExtraction.extractForADHDFCBK(body, address, dateTime, timestampMillis);
                }
                else if ("JX-HDFCBK".equalsIgnoreCase(address)) {
                    transactionDetailsList = DataExtraction.extractForJDBoiInd(body, address, dateTime, timestampMillis);
                }


                if (transactionDetailsList != null) {
                    smsList.addAll(transactionDetailsList);
                }

            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        // Sort messages by date and time
        Collections.sort(smsList);

        // Notify the adapter of data changes
        ArrayAdapter<SMSMessage> adapter = (ArrayAdapter<SMSMessage>) listView.getAdapter();
        adapter.notifyDataSetChanged();
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

        String userId = user.getUid();

        try {
            // Prepare data to save
            Map<String, Object> smsData = new HashMap<>();
            for (SMSMessage sms : smsList) {
                // Encrypt each message using AES
                String encryptedMessage = AESEncryption.encrypt(sms.toString());
                String key = databaseReference.child("user").child(userId).child("messages").push().getKey();
                smsData.put(key, encryptedMessage);
            }

            // Save encrypted data to Firebase
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



}