package com.example.game2048;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GameActivity extends AppCompatActivity implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {
    Game g;
    ImageView[][] imageField;
    ArrayDeque<int[][]> fieldBackDeque;
    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;
    private GestureDetectorCompat mDetector;
    MediaPlayer mPlayer;
    MotionEvent eventMotionStart;
    int maxValue = 8 * 8 + 2;

    Bitmap[] bitmapImages;

    float sizeCell, sizeSpace;

    float ratioOfSize = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mPlayer = MediaPlayer.create(this, R.raw.sound_creatinon);
        settings = getSharedPreferences("GameSettings", MODE_PRIVATE);
        prefEditor = settings.edit();
        fieldBackDeque = new ArrayDeque<>();
        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);
        bitmapImages = new Bitmap[maxValue];
        for (int i = 0; i < maxValue; ++i)
            bitmapImages[i] = createBitmap(i);

        ((ConstraintLayout) findViewById(R.id.gameField)).post(() -> {

            if (!tryLoadGame()) {
                restartGame((View) (findViewById(R.id.buttonRestart)));
            }

            ((TextView) findViewById(R.id.TextViewScore)).setText("Очки: " + g.score);
            ((TextView) findViewById(R.id.TextViewMaxScore)).setText("Рекорд: " + g.maxScore);

        });
    }

    private Bitmap createBitmap(int value) {
        int[] size = getSizeField();
        float m = (float) size[0] * size[1] + 1;
        float[] hsv = new float[]{
                90 * (1 - (float) value / m), 0.75f, 1f};
        int backgroundColor = Color.HSVToColor(hsv);
        String text = "";
        Paint mPaint = new Paint();
        int radius = 50;
        if (value == 0) {
            text = "";
            backgroundColor = Color.parseColor("#a0a0a0");
        } else if (value == -1) {
            radius = 10;
            backgroundColor = Color.parseColor("#CDBCBC");
        } else if (value <= 15) {
            text = String.valueOf(1L << value);
            mPaint.setTextSize(180);
        } else if (19 > value) {
            text = String.valueOf(1L << value);
            mPaint.setTextSize(150);
        } else {
            text = "2^" + value;
            mPaint.setTextSize(180);
        }
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mPaint.setColor(backgroundColor);
        canvas.drawRoundRect(0, 0, 500, 500, radius, radius, mPaint);

        mPaint.setColor(Color.BLACK);
        Rect mTextBoundRect = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), mTextBoundRect);
        canvas.drawText(text, 250 - (mPaint.measureText(text) / 2f),
                250 + (mTextBoundRect.height() / 2f), mPaint);

        return bitmap;
    }

    private boolean tryLoadGame() {
        // Проверка на сохранненую игру
        int[] xy = getSizeField();
        String name = "Pole" + xy[0] + "x" + xy[1];
        if (!settings.getBoolean(name, false)) {
            Toast.makeText(getApplicationContext(), "Приветсвую!", Toast.LENGTH_SHORT).show();
            return false;
        } // ее нет
        Toast.makeText(getApplicationContext(), "Продолжим игру", Toast.LENGTH_SHORT).show();
        clearAll();
        g = new Game(xy[0], xy[1]);
        sizeCell = ((ConstraintLayout) findViewById(R.id.gameField)).getWidth() / (g.w + ratioOfSize * (g.w + 1));
        sizeSpace = ratioOfSize * sizeCell;
        imageField = new ImageView[xy[1]][xy[0]];
        for (int i = 0; i < g.h; ++i)
            for (int j = 0; j < g.w; ++j) {
                g.field[i][j] = settings.getInt(name + i + j, 0);
            }
        g.score = settings.getLong(name + "score", 0);
        g.maxScore = settings.getLong(name + "maxScore", 0);
        fromGameFieldDoImageField();
        return true;
    }

    public void restartGame(View view) {
        clearAll();
        int[] xy = getSizeField();
        g = new Game(xy[0], xy[1]);
        g.maxScore = settings.getLong("Pole" + xy[0] + "x" + xy[1] + "maxScore", 0);
        sizeCell = ((ConstraintLayout) findViewById(R.id.gameField)).getWidth() / (g.w + ratioOfSize * (g.w + 1));
        sizeSpace = ratioOfSize * sizeCell;
        imageField = new ImageView[xy[1]][xy[0]];
        fromGameFieldDoImageField();
    }

    int[] getSizeField() {
        if (settings.getInt("LastFieldX", -1) == -1) {
            prefEditor.putInt("LastFieldX", 4);
            prefEditor.putInt("LastFieldY", 4);
            prefEditor.apply();
        }
        return new int[]{settings.getInt("LastFieldX", 4),
                settings.getInt("LastFieldY", 4)};// x, y
    }


    public void fromGameFieldDoImageField() {
        fieldBackDeque.addLast(g.copyField());
        ConstraintLayout cL = (ConstraintLayout) findViewById(R.id.gameField);
        for (int i = 0; i < g.h; ++i)
            for (int j = 0; j < g.w; ++j) {
                ImageView img = new ImageView(this);
                img.setLayoutParams(new LinearLayout.LayoutParams((int) sizeCell, (int) sizeCell));
                img.setImageBitmap(bitmapImages[0]);
                img.setX(sizeSpace + (sizeSpace + sizeCell) * j);
                img.setY(sizeSpace + (sizeSpace + sizeCell) * i);
                cL.addView(img);

                img = new ImageView(this);
                imageField[i][j] = img;
                img.setLayoutParams(new LinearLayout.LayoutParams((int) sizeCell, (int) sizeCell));
                img.setX(sizeSpace + (sizeSpace + sizeCell) * j);
                img.setY(sizeSpace + (sizeSpace + sizeCell) * i);
                cL.addView(img);
            }
        updateImage();
    }

    public void updateImage() {
        for (int i = 0; i < g.h; ++i)
            for (int j = 0; j < g.w; ++j)
                imageField[i][j].setImageBitmap(bitmapImages[g.field[i][j]]);
    }

    private void saveGame() {
        String name = "Pole" + g.w + "x" + g.h;
        prefEditor.putBoolean(name, true);
        for (int i = 0; i < g.h; ++i)
            for (int j = 0; j < g.w; ++j) {
                prefEditor.putInt(name + i + j, g.field[i][j]);
            }
        prefEditor.putLong(name + "score", g.score);
        prefEditor.putLong(name + "maxScore", g.maxScore);
        prefEditor.apply();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) { // Обработка нажатия
        if (eventMotionStart != null && eventMotionStart.getDownTime() == event.getDownTime() && event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - eventMotionStart.getX(), dy = event.getY() - eventMotionStart.getY();
            if (Math.abs(dx) > 50 || Math.abs(dy) > 50) {
                if (dx != 0 && Math.abs(dy / dx) < 0.3f) {
                    if (dx > 0) click("right");
                    else click("left");
                } else if (dy != 0 && Math.abs(dx / dy) < 0.3f) {
                    if (dy > 0) click("down");
                    else click("up");
                }
            }
        }
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void click(String direction) { // Обработка нажатия
        ArrayList<int[]> instructions = g.moveEvent(direction);
        if (g.maxScore < g.score) {
            g.maxScore = g.score;
            ((TextView) findViewById(R.id.TextViewMaxScore)).setText("Рекорд: " + g.maxScore + "\uD83C\uDFC6");
        }
        ((TextView) findViewById(R.id.TextViewScore)).setText("Очки: " + g.score);
        for (int k = 0; k < instructions.size(); ++k) {
            int[] xy1xy2 = instructions.get(k);
            imageField[xy1xy2[1]][xy1xy2[0]].bringToFront();
            imageField[xy1xy2[1]][xy1xy2[0]].startAnimation(getTranslateAnimationFromxy1xy2(xy1xy2));
        } // запуск анимаций по инструкциям

        if (g.gameNotLose()) {
            if (instructions.size() != 0) {
                imageField[instructions.get(0)[1]][instructions.get(0)[0]].postDelayed(() -> {
                    updateImage();
                    int xy = g.addNum();
                    mPlayer.start();
                    if (xy != -1) {
                        fieldBackDeque.addLast(g.copyField());
                        if (fieldBackDeque.size() > 10) fieldBackDeque.removeFirst();
                        imageField[xy / g.h][xy % g.h].startAnimation(getAnimationScaleFromXY(xy));
                        imageField[xy / g.h][xy % g.h].setImageBitmap(bitmapImages[g.field[xy / g.h][xy % g.h]]);
                    }
                }, 300);
                saveGame();

            }
        } else {
            Toast.makeText(getApplicationContext(), "Конец игры...", Toast.LENGTH_SHORT).show();
            loadMaxScoreInDB();
            restartGame((View) findViewById(R.id.buttonRestart));
        }
    }

    private void loadMaxScoreInDB() {
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
                    .add("id", Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID))
                    .add("field", g.w + "x" + g.h)
                    .add("score", String.valueOf(g.maxScore))
                    .build();
            Request request = new Request.Builder()
                    .url(settings.getString("url", ""))
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();
            response.body().string();
        } catch (SocketTimeoutException e) {
            Toast.makeText(getApplicationContext(), "Превышено время ожидания", Toast.LENGTH_SHORT).show();
            prefEditor.putBoolean("connectSuccess", false);
        } catch (IOException e) {
            prefEditor.putBoolean("connectSuccess", false);
        } catch (Exception e) {
            prefEditor.putBoolean("connectSuccess", false);
        }

    }

    public void stepBackField(View view) { // Кнопка StepBack
        if (fieldBackDeque.size() <= 1)
            return;
        fieldBackDeque.removeLast();
        Toast.makeText(getApplicationContext(), "Шаг назад", Toast.LENGTH_SHORT).show();
        for (int i = 0; i < g.h; ++i)
            g.field[i] = fieldBackDeque.getLast()[i].clone();
        updateImage();
    }

    void clearAll() {
        ((TextView) findViewById(R.id.TextViewScore)).setText("Очки: 0");
        ConstraintLayout constraintLayout = ((ConstraintLayout) findViewById(R.id.gameField));
        constraintLayout.removeAllViews();
        {
            ImageView img = new ImageView(this);
            img.setLayoutParams(new LinearLayout.LayoutParams(constraintLayout.getWidth(), constraintLayout.getHeight()));
            img.setImageBitmap(createBitmap(-1));
            constraintLayout.addView(img);
        }


        fieldBackDeque.clear();
    }

    public void closeGame(View view) {
        saveGame();
        loadMaxScoreInDB();
        this.finish();
    }


    ScaleAnimation getAnimationScaleFromXY(int xy) {
        int j = xy % g.w;
        int i = xy / g.h;

        ScaleAnimation animationScale = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.ABSOLUTE, (sizeSpace + sizeSpace * j + sizeCell * j + 0.5f * sizeCell),
                Animation.ABSOLUTE, (sizeSpace + sizeSpace * i + sizeCell * i + 0.5f * sizeCell));
        animationScale.setDuration(300);
        return animationScale;
    }

    TranslateAnimation getTranslateAnimationFromxy1xy2(int[] xy1xy2) {

        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, (sizeSpace + sizeCell) * (xy1xy2[2] - xy1xy2[0]) / sizeCell,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, (sizeSpace + sizeCell) * (xy1xy2[3] - xy1xy2[1]) / sizeCell
        );
        animation.setDuration(300);

        return animation;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return false;
    }

    public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1,
                            float v, float v1) {
        eventMotionStart = motionEvent;
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
    }

    @Override
    public boolean onFling(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1,
                           float v, float v1) {
        eventMotionStart = motionEvent;
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent e) {
        return false;
    }
}