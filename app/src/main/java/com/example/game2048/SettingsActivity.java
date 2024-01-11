package com.example.game2048;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {
    SharedPreferences settings;
    TextView textEditName;
    SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        textEditName = findViewById(R.id.editTextName);
        settings = getSharedPreferences("GameSettings", MODE_PRIVATE);
        prefEditor = settings.edit();
        textEditName.setText(settings.getString("PlayerName", null));
        {
            LinearLayout layout = (LinearLayout) findViewById(R.id.layoutButtonSizeField);
            int x = settings.getInt("LastFieldX", 4);
            int y = settings.getInt("LastFieldY", 4);
            for (int i = 0; i < layout.getChildCount(); ++i)
                if (((Button) layout.getChildAt(i)).getText().toString().equals(x + "x" + y))
                    layout.getChildAt(i).setAlpha(1);
                else layout.getChildAt(i).setAlpha(0.3f);
        }
    }


    public void applyChanges(View view) {
        prefEditor.putString("PlayerName", textEditName.getText().toString());
        prefEditor.putString("url", getString(R.string.url));
        prefEditor.putBoolean("connectSuccess", true);
        prefEditor.apply();

        updateNameOnDB();

        finish();
    }

    private void updateNameOnDB() {
        try {
            if (!settings.getBoolean("connectSuccess", false)) return;
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(500, TimeUnit.MILLISECONDS)
                    .writeTimeout(500, TimeUnit.MILLISECONDS)
                    .readTimeout(500, TimeUnit.MILLISECONDS)
                    .build();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            RequestBody formBody = new FormBody.Builder()
                    .add("updateName", settings.getString("PlayerName", ""))
                    .add("id", Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID))
                    .build();
            Request request = new Request.Builder()
                    .url(settings.getString("url", ""))
                    .post(formBody)
                    .build();
            Response response = client.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            Toast.makeText(getApplicationContext(), "Превышено время ожидания", Toast.LENGTH_SHORT).show();
            prefEditor.putBoolean("connectSuccess", false);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Ошибка соединения", Toast.LENGTH_SHORT).show();
            prefEditor.putBoolean("connectSuccess", false);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Ошибка!", Toast.LENGTH_SHORT).show();
            prefEditor.putBoolean("connectSuccess", false);
        }


    }

    public void clearSettings(View view) {
        textEditName.setText(null);
        prefEditor.clear();
        changeSizeField(findViewById(R.id.buttonPole4x4));
        prefEditor.apply();

    }

    public void changeSizeField(View view) {
        int[] sizeField = new int[]{4, 4};
        LinearLayout layout = findViewById(R.id.layoutButtonSizeField);
        for (int i = 0; i < layout.getChildCount(); ++i)
            if (((Button) layout.getChildAt(i)).getText().toString().equals(((Button) view).getText().toString()))
                layout.getChildAt(i).setAlpha(1);
            else layout.getChildAt(i).setAlpha(0.3f);

        if (view.getId() == R.id.buttonPole4x4)
            sizeField = new int[]{4, 4};
        else if (view.getId() == R.id.buttonPole5x5)
            sizeField = new int[]{5, 5};
        else if (view.getId() == R.id.buttonPole6x6)
            sizeField = new int[]{6, 6};
        else if (view.getId() == R.id.buttonPole7x7)
            sizeField = new int[]{7, 7};
        else if (view.getId() == R.id.buttonPole8x8)
            sizeField = new int[]{8, 8};
        prefEditor.putInt("LastFieldX", sizeField[0]);
        prefEditor.putInt("LastFieldY", sizeField[1]);
    }
}