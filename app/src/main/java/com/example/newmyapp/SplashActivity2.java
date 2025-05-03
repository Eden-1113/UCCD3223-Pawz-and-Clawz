package com.example.newmyapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity2 extends AppCompatActivity {

    private ImageView splashLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash2);

        splashLogo = findViewById(R.id.splashLogo);

        // Load scale animation
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        splashLogo.startAnimation(scaleUp);

        // Go to LoginActivity after 3 seconds
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity2.this, LoginActivity.class));
            finish(); // close splash
        }, 3000);
    }
}
