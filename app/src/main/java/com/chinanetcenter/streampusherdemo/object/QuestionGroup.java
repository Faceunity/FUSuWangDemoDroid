package com.chinanetcenter.streampusherdemo.object;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * {
 *      userType:0,//表示题目采用内部Demo模块
 *      questionList:
 *      [
 *          {
 *              "question":"2008年在北京举行的是第几届夏季奥林匹克运动会",
 *              "answer": ["29","30","31"],
 *              "correct": [0],//正确答案要选项中的索引
 *              "type":  1, //单选
 *          },
 *          {
 *              "question":"2008年在北京举行的是第几届夏季奥林匹克运动会",
 *              "answer": ["29","30","31"],
 *              "correct": [0,1],//正确答案要选项中的索引
 *              "type":  2, //多选
 *          }
 *      ]
 * }
 */
public class QuestionGroup {
    public static final String KEY_USER_TYPE = "userType";
    public static final String KEY_QUSTION_LIST = "questionList";

    public ArrayList<Question> questionList = null;
    public int userType;

    public QuestionGroup(){}

    public static QuestionGroup parseFromJsonString(String jsonString) {
        QuestionGroup questionGroup = new QuestionGroup();
        try {
            JSONObject groupJsonObject = new JSONObject(jsonString);
            questionGroup.userType = groupJsonObject.getInt(KEY_USER_TYPE);
            JSONArray questionJsonArray = groupJsonObject.getJSONArray(KEY_QUSTION_LIST);
            questionGroup.questionList = new ArrayList<>(questionJsonArray.length());
            for(int i = 0; i < questionJsonArray.length(); i++) {
                JSONObject questionJsonObject = (JSONObject) questionJsonArray.get(i);
                Question question = Question.parseFromJson(questionJsonObject);
                if(question != null) {
                    questionGroup.questionList.add(question);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return questionGroup;
    }

    @Override
    public String toString() {
        JSONObject jsonObjectGroup = new JSONObject();
        try {
            jsonObjectGroup.put(KEY_USER_TYPE,0);
            JSONArray jsonArray = new JSONArray();
            if(questionList != null && !questionList.isEmpty()) {
                for (int i = 0; i < questionList.size(); i++) {
                    jsonArray.put(questionList.get(i).toJsonObject());
                }
            }
            jsonObjectGroup.put(KEY_QUSTION_LIST, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObjectGroup.toString();
    }
}
