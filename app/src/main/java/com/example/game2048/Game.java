package com.example.game2048;

import java.util.ArrayList;
import java.util.Random;

public class Game {
    int w, h;
    int[][] field;
    Random rand = new Random();
    long score = 0, maxScore = 0;

    Game(int wight, int height) {
        w = wight;
        h = height;
        field = new int[h][w];
        addNum();
        addNum();
    }

    boolean gameNotLose() {
        for (int i = 0; i < h; ++i)
            for (int j = 0; j < w; ++j)
                if (field[i][j] == 0 ||
                        i > 0 && field[i - 1][j] == field[i][j] ||
                        i + 1 < h && field[i + 1][j] == field[i][j] ||
                        j > 0 && field[i][j - 1] == field[i][j] ||
                        j + 1 < w && field[i][j + 1] == field[i][j])
                    return true;
        return false;
    }

    int addNum() { // возвращает позицию вставки нового числа
        int countZero = 0;
        for (int i = 0; i < h; ++i)
            for (int j = 0; j < w; ++j)
                if (field[i][j] == 0) countZero++;
        if (countZero == 0) return -1;
        int xy = rand.nextInt(countZero);
        countZero = 0;
        for (int i = 0; i < w * h; ++i) {
            if (field[i / w][i % w] == 0 && countZero == xy) {
                if (rand.nextInt(10) == 0)
                    field[i / w][i % w] = 2;
                else
                    field[i / w][i % w] = 1;
                return i;
            }
            if (field[i / w][i % w] == 0) countZero++;
        }
        return -1;
    }

    public ArrayList<int[]> moveEvent(String direction) {
        ArrayList<int[]> instructions = new ArrayList<int[]>();
        switch (direction) {
            case "left":
                for (int i = 0; i < h; ++i) {
                    ArrayList<int[]> temp = makeLeft(field[i]);
                    for (int k = 0; k < temp.size(); k++) {
                        instructions.add(new int[]{temp.get(k)[0], i, temp.get(k)[1], i});
                    }
                }
                break;
            case "right":
                for (int i = 0; i < h; ++i) {
                    int[] arr = field[i].clone();
                    reverseArr(arr);
                    ArrayList<int[]> temp = makeLeft(arr);
                    reverseArr(arr);
                    field[i] = arr;
                    for (int k = 0; k < temp.size(); k++) {
                        instructions.add(new int[]{arr.length - temp.get(k)[0] - 1, i, arr.length - temp.get(k)[1] - 1, i});
                    }
                }
                break;
            case "up":
                for (int j = 0; j < w; ++j) {
                    int[] arr = new int[h];
                    for (int i = 0; i < h; ++i) arr[i] = field[i][j];
                    ArrayList<int[]> temp = makeLeft(arr);
                    for (int i = 0; i < h; ++i) field[i][j] = arr[i];
                    for (int k = 0; k < temp.size(); k++) {
                        instructions.add(new int[]{j, temp.get(k)[0], j, temp.get(k)[1]});
                    }
                }
                break;
            case "down":
                for (int j = 0; j < w; ++j) {
                    int[] arr = new int[h];
                    for (int i = 0; i < h; ++i) arr[h - i - 1] = field[i][j];
                    ArrayList<int[]> temp = makeLeft(arr);
                    for (int i = 0; i < h; ++i) field[i][j] = arr[h - i - 1];
                    for (int k = 0; k < temp.size(); k++) {
                        instructions.add(new int[]{j, h - temp.get(k)[0] - 1, j, h - temp.get(k)[1] - 1});
                    }
                }
                break;
        }
        return instructions;

    }

    public void reverseArr(int[] arr) {
        for (int k = 0, t; k < arr.length / 2; ++k) {
            t = arr[k];
            arr[k] = arr[arr.length - k - 1];
            arr[arr.length - k - 1] = t;
        }
    }

    private ArrayList<int[]> makeLeft(int[] arr) {
        ArrayList<int[]> inst = new ArrayList<int[]>();
        boolean[] canNotUnion = new boolean[arr.length];
        for (int j = 1; j < w; ++j) {
            if (arr[j] != 0) {
                int ind = j;
                while (ind > 0 && arr[ind - 1] == 0) ind--;
                if (ind != 0 && arr[j] == arr[ind - 1] && (!canNotUnion[ind - 1])) {
                    arr[ind - 1]++;
                    score += Math.pow(2, arr[ind - 1]);
                    canNotUnion[ind - 1] = true;
                    arr[j] = 0;
                    inst.add(new int[]{j, ind - 1});
                } else {
                    if (ind != j) {
                        arr[ind] = arr[j];
                        arr[j] = 0;
                        inst.add(new int[]{j, ind});
                    }
                }
            }
        }
        return inst;
    }

    public int[][] copyField() {
        int[][] fieldCopy = new int[h][w];
        for (int i = 0; i < h; ++i) fieldCopy[i] = field[i].clone();
        return fieldCopy;
    }
}
