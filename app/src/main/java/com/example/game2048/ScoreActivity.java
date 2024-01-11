package com.example.game2048;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScoreActivity extends AppCompatActivity {
    String playerName = null, fieldSize = "4x4";
    Long maxScore = null;
    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settings = getSharedPreferences("GameSettings", MODE_PRIVATE);
        prefEditor = settings.edit();
        run();
    }

    public void run() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.layoutButtonSizeField);
        for (int i = 0; i < layout.getChildCount(); ++i)
            if (((Button) layout.getChildAt(i)).getText().toString() == fieldSize)
                layout.getChildAt(i).setAlpha(1);
            else layout.getChildAt(i).setAlpha(0.3f);
        playerName = settings.getString("PlayerName", null);
        maxScore = settings.getLong("Pole" + fieldSize + "maxScore", -1);
        List<Object[]> arrayScoreName = getDataFromDB();
        TableLayout tableLayout = findViewById(R.id.tableLayoutScore);
        TableRow tableRow = findViewById(R.id.tableRowExample);
        int maxSizeTable = 10, place = 1;
        boolean playerOnScoreList = false;
        for (int i = 0; i < maxSizeTable && i < arrayScoreName.size(); i++) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            String name = (String) arrayScoreName.get(i)[1];
            Long score = (Long) arrayScoreName.get(i)[0];
            if (Objects.equals(name, playerName)) playerOnScoreList = true;
            if (!playerOnScoreList && maxScore >= score) {
                playerOnScoreList = true;
                row.addView(createTextView(String.valueOf(place)), findViewById(R.id.textViewPlace).getLayoutParams());
                row.addView(createTextView(playerName), findViewById(R.id.textViewScore).getLayoutParams());
                row.addView(createTextView(String.valueOf(maxScore)), findViewById(R.id.textViewName).getLayoutParams());
                row.setWeightSum(tableRow.getWeightSum());
                tableLayout.addView(row);
                row = new TableRow(this);
                row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                if (maxScore > score) place++;
            }
            row.addView(createTextView(String.valueOf(place)), findViewById(R.id.textViewPlace).getLayoutParams());
            row.addView(createTextView(name), findViewById(R.id.textViewScore).getLayoutParams());
            row.addView(createTextView(String.valueOf(score)), findViewById(R.id.textViewName).getLayoutParams());
            row.setWeightSum(tableRow.getWeightSum());
            tableLayout.addView(row);
            if (i + 1 < arrayScoreName.size()) {
                Long score2 = (Long) arrayScoreName.get(i + 1)[0];
                if (score2 < score) place++;
            }
        }

        if (!playerOnScoreList) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            row.addView(createTextView(String.valueOf(">" + place)), findViewById(R.id.textViewPlace).getLayoutParams());
            row.addView(createTextView(playerName), findViewById(R.id.textViewScore).getLayoutParams());
            row.addView(createTextView(String.valueOf(maxScore)), findViewById(R.id.textViewName).getLayoutParams());
            row.setWeightSum(tableRow.getWeightSum());
            tableLayout.addView(row);
        }
    }

    private TextView createTextView(String str) {
        TextView textView = new TextView(getApplicationContext());
        textView.setText(str);
        textView.setTextSize(18);
        textView.setTextColor(Color.BLACK);
        return textView;
    }

    public List<Object[]> getDataFromDB() {
        List<Object[]> arr = new ArrayList<>();
        try {
            if (!settings.getBoolean("connectSuccess", false)) return arr;
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(500, TimeUnit.MILLISECONDS)
                    .writeTimeout(500, TimeUnit.MILLISECONDS)
                    .readTimeout(500, TimeUnit.MILLISECONDS)
                    .build();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            RequestBody formBody = new FormBody.Builder()
                    .add("field", fieldSize)
                    .build();
            Request request = new Request.Builder()
                    .url(settings.getString("url", ""))
                    .post(formBody)
                    .build();
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();
            JSONObject json = new JSONObject(resStr);
            for (Iterator<?> keyIter = json.keys(); keyIter.hasNext(); ) {
                String name = keyIter.next().toString();
                JSONArray obj = (JSONArray) json.get(name);
                for (int i = 0; i < obj.length(); i++) {
                    Long score = Long.valueOf((((JSONObject) obj.get(i)).get("score")).toString());
                    Object[] temp = new Object[]{score, name};
                    arr.add(temp);
                }

            }
            Collections.sort(arr, new Comparator<Object[]>() {
                @Override
                public int compare(Object[] lhs, Object[] rhs) {
                    return Long.compare((Long) rhs[0], (Long) lhs[0]);
                }
            });


        } catch (SocketTimeoutException e) {
            Toast.makeText(getApplicationContext(), "Время ожидания превышено", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            Toast.makeText(getApplicationContext(), "Не удалось получить чужие рекорды!", Toast.LENGTH_SHORT).show();
        }
        return arr;
    }

    public void changeSizeField(View view) {
        fieldSize = ((Button) view).getText().toString();
        TableLayout tableLayout = findViewById(R.id.tableLayoutScore);
        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
        run();
    }
}