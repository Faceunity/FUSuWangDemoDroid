package com.chinanetcenter.streampusherdemo.object;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class QuestionUtils {

    public static QuestionGroup loadTestQuestions(Context context) {
        if (context == null)
            return null;
        File sdcardFile = new File("/sdcard/questions");

        InputStream inputStream = null;
        QuestionGroup questions = null;

        //加载sd卡上测试题目
        if(!sdcardFile.isDirectory() && sdcardFile.exists()) {
            try {
                inputStream = new FileInputStream(sdcardFile);
                questions = parseQuestions(inputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                        inputStream = null;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        //加载内置测试题目
        if(questions == null) {
            try {
                inputStream = context.getAssets().open("questions");
                questions = parseQuestions(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return questions;
    }

    private static QuestionGroup parseQuestions(InputStream inputStream) {
        StringBuilder questionString = new StringBuilder();
        String s;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
            while((s = bufferedReader.readLine())!=null){
                questionString.append(s).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return QuestionGroup.parseFromJsonString(questionString.toString());
    }
}
