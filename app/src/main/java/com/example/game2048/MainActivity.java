package com.example.game2048;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;

    /*Pole4x4 - bool -> существует ли поле
     * Pole4x433 - int -> значение в ячейке 3 3 в поле 4 4
     * LastFieldX, LastFieldY - размеры поля которые заданны пользователям для игры
     * Pole4x4score - long
     * Pole4x4maxScore - long
     * PlayerName - string
     * connectSuccess - boolean
     * */
    Intent settingsIntent, gameIntent, scoreIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        settings = getSharedPreferences("GameSettings", MODE_PRIVATE);
        prefEditor = settings.edit();
        prefEditor.putString("url", getString(R.string.url));
        prefEditor.putBoolean("connectSuccess", true);
        prefEditor.apply();
        settingsIntent = new Intent(this, SettingsActivity.class);
        gameIntent = new Intent(this, GameActivity.class);
        scoreIntent = new Intent(this, ScoreActivity.class);
    }

    public void start_game(View view) {
        startActivity(gameIntent);
    }

    public void click_settings(View view) {
        startActivity(settingsIntent);
    }

    public void click_maxScore(View view) {
        startActivity(scoreIntent);
    }
}
