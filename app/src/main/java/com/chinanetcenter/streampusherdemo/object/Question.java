package com.chinanetcenter.streampusherdemo.object;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {
 * "question":"2008年在北京举行的是第几届夏季奥林匹克运动会",
 * "answer": ["29","30","31"],
 * "correct": [0],//正确答案要选项中的索引
 * "type":  1, //单选
 * }
 */
public class Question {
    public static final String KEY_QUETION = "question";
    public static final String KEY_ANSWER = "answer";
    public static final String KEY_CORRECT = "correct";
    public static final String KEY_TYPE = "type";

    public static final int TYPE_SINGLE_CHOICE = 1;
    public static final int TYPE_MULTI_CHOICE = 2;

    public String question;
    public String answer[];
    public int correct[];
    public int type;

    private Question() {}


    public static Question parseFromJson(JSONObject jsonObject) {
        if(jsonObject == null) return null;
        Question question = new Question();
        try {
            question.question = jsonObject.getString(KEY_QUETION);
            JSONArray answer = jsonObject.getJSONArray(KEY_ANSWER);
            question.answer = new String[answer.length()];
            for(int i= 0; i < answer.length(); i++) {
                question.answer[i] = answer.getString(i);
            }
            JSONArray correct = jsonObject.getJSONArray(KEY_CORRECT);
            question.correct = new int[correct.length()];
            for(int i= 0; i < correct.length(); i++) {
                question.correct[i] = correct.getInt(i);
            }
            question.type = jsonObject.getInt(KEY_TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return question;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_QUETION, question);
            jsonObject.put(KEY_ANSWER, answer);
            jsonObject.put(KEY_CORRECT, correct);
            jsonObject.put(KEY_TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_QUETION, question);
            JSONArray jsonArray = new JSONArray();
            if(answer != null) {
                for(int i= 0; i < answer.length; i++) {
                    jsonArray.put(answer[i]);
                }
            }
            jsonObject.put(KEY_ANSWER, jsonArray);
            jsonArray = new JSONArray();
            if(correct != null) {
                for(int i= 0; i < correct.length; i++) {
                    jsonArray.put(correct[i]);
                }
            }
            jsonObject.put(KEY_CORRECT, jsonArray);
            jsonObject.put(KEY_TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObject;
    }
}
