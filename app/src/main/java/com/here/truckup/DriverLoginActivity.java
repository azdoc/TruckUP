package com.here.truckup;

import android.content.Intent;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;
    private TextView mForgotpassword;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);

        mLogin = (Button) findViewById(R.id.login);
        mRegistration = (Button) findViewById(R.id.registration);
        mForgotpassword = (TextView) findViewById(R.id.mforgotpassword) ;
        progressBar =(ProgressBar) findViewById(R.id.simpleProgressBar);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null)
                {
                    Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                if (email.length() == 0){
                    progressBar.setVisibility(View.INVISIBLE);
                    mEmail.setError("Enter an email address");
                    return;
                }
                if (password.length() < 6){
                    progressBar.setVisibility(View.INVISIBLE);
                    mPassword.setError("Password must be at least 6 characters");
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(DriverLoginActivity.this, "Sign up Error", Toast.LENGTH_SHORT).show();
                        }else{
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(user_id).child("name");
                            current_user_db.setValue(email);
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();

                if (email.length() == 0){
                    progressBar.setVisibility(View.INVISIBLE);
                    mEmail.setError("Enter an email address");
                    return;
                }
                if (password.length() < 6){
                    progressBar.setVisibility(View.INVISIBLE);
                    mPassword.setError("Password must be at least 6 characters");
                    return;
                }
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(DriverLoginActivity.this, "Sign up Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

        mForgotpassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverLoginActivity.this, ForgotpasswordActivity.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
        progressBar.setVisibility(View.INVISIBLE);
    }
    @Override
    protected void onStop() {
        super.onStop();
        if(firebaseAuthListener != null){                                       //added line
            mAuth.removeAuthStateListener(firebaseAuthListener);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}