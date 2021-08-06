package com.chinanetcenter.streampusherdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.chinanetcenter.streampusherdemo.MyApp;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.utils.PreferenceUtil;

import java.io.IOException;
import java.io.InputStream;

public class NeedFaceUnityAcct extends AppCompatActivity {

    private boolean isOn = true;//是否使用FaceUnity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_faceunity);

        final Button button = (Button) findViewById(R.id.btn_set);
        String isOpen = PreferenceUtil.getString(MyApp.getMyInstance(), PreferenceUtil.KEY_FACEUNITY_ISON);
        if (TextUtils.isEmpty(isOpen) || "false".equals(isOpen)) {
            isOn = false;
        } else {
            isOn = true;
        }
        button.setText(isOn ? "On" : "Off");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOn = !isOn;
                button.setText(isOn ? "On" : "Off");
            }
        });

        Button btnToMain = (Button) findViewById(R.id.btn_to_main);
        btnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NeedFaceUnityAcct.this, ConfigActivity.class);
                PreferenceUtil.persistString(MyApp.getMyInstance(), PreferenceUtil.KEY_FACEUNITY_ISON,
                        isOn + "");
                startActivity(intent);
                finish();
            }
        });

        try {
            InputStream ins = getAssets().open("makeup/naicha.bundle");
            Log.e("benyq", "onCreate: ins " + ins.available());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
