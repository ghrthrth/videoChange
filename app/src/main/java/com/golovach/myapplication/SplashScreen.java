package com.golovach.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashScreen extends AppCompatActivity {
    private static final int DELAY_MILLIS = 3000;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mAuth = FirebaseAuth.getInstance();
        // Check if the user is already signed in

        new Handler().postDelayed(() -> {
                    if (mAuth.getCurrentUser() != null) {
                        // Получаем текущую информацию о пользователе
                        SharedPreferences sharedPreferences = getSharedPreferences("MyApp", MODE_PRIVATE);
                        boolean isLogged = sharedPreferences.getBoolean("isLogged", true);

                        if (isLogged) {
                            startActivity(new Intent(SplashScreen.this, MainActivity.class));
                        } else {
                            SplashScreen.this.startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                        }
                        finish();
                    } else {
                        SplashScreen.this.startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                        finish();
                    }
                },
                DELAY_MILLIS);
    }
}

