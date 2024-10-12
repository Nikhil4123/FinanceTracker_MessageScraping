package com.example.finandetails;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finandetails.databinding.ActivitySignUpBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class Sign_UP extends AppCompatActivity {
    private static final int RC_SIGN_IN = 20;
    private static final String TAG = "Sign_UP";

    ActivitySignUpBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    GoogleSignInClient googleSignInClient;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase and Google Sign-In
        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing up...");
        progressDialog.setCancelable(false);

        // Google Sign-In button click listener
        binding.googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        // Email/Password sign-up button click listener
        binding.signupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameET.getText().toString().trim();
                String profession = binding.professionET.getText().toString().trim();
                String email = binding.emailET.getText().toString().trim();
                String password = binding.passwordET.getText().toString().trim();

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
                database.getReference().child("user").orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            progressDialog.dismiss();
                            showAlertDialog("This name is already in use. Please choose a different name.");
                        } else {
                            createUserWithEmailAndPassword(name, profession, email, password, buildNumber, modelNumber, androidVersion);
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
                Intent intent = new Intent(Sign_UP.this, LogIn.class);
                startActivity(intent);
            }
        });
    }

    private void createUserWithEmailAndPassword(String name, String profession, String email, String password, String buildNumber, String modelNumber, String androidVersion) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    User newUser = new User(name, profession, email, password, buildNumber, modelNumber, androidVersion);
                    String id = user.getUid();
                    database.getReference().child("user").child(id).setValue(newUser);
                    Toast.makeText(Sign_UP.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Sign_UP.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Sign_UP.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed", e);
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", user.getUid());
                    map.put("name", user.getDisplayName());
                    map.put("profile", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                    database.getReference().child("users").child(user.getUid()).setValue(map);

                    Intent intent = new Intent(Sign_UP.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Sign_UP.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Sign_UP.this);
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
