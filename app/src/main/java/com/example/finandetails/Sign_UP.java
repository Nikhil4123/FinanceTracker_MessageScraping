package com.example.finandetails;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.finandetails.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Sign_UP extends AppCompatActivity {
ActivitySignUpBinding binding;

    FirebaseAuth auth;
    FirebaseDatabase database;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    String verificationId;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        FirebaseApp.initializeApp(this);
        auth =FirebaseAuth.getInstance();
        database =FirebaseDatabase.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing up...");
        progressDialog.setCancelable(false);

        binding.signupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameET.getText().toString();
                String profession = binding.professionET.getText().toString();
                String email = binding.emailET.getText().toString();
                String password = binding.passwordET.getText().toString();

                String buildNumber = Build.VERSION.INCREMENTAL;
                String modelNumber = Build.MODEL;
                String androidVersion = Build.VERSION.RELEASE;

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(profession) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(Sign_UP.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!email.endsWith("@gmail.com")) {
                    Toast.makeText(Sign_UP.this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(Sign_UP.this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();
//                String email =binding.emailET.getText().toString(), password = binding.passwordET.getText().toString();
                database.getReference().child("user").orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Name already exists, show toast
                            progressDialog.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(Sign_UP.this);
                            builder.setMessage("This name is already in use. Please choose a different name.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // You can add functionality here if needed
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        } else {
                            // Name is available, proceed with sign up
                            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        User user = new User(name, profession, email, password , buildNumber, modelNumber, androidVersion);



                                        String id = task.getResult().getUser().getUid();
                                        database.getReference().child("user").child(id).setValue(user);
                                        progressDialog.dismiss();
                                        Toast.makeText(Sign_UP.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Sign_UP.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // Sign up failed
                                        progressDialog.dismiss();
                                        Toast.makeText(Sign_UP.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressDialog.dismiss();
                        Toast.makeText(Sign_UP.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.gotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =  new Intent(Sign_UP.this, LogIn.class);
                startActivity(intent);
            }
        });


    }
}