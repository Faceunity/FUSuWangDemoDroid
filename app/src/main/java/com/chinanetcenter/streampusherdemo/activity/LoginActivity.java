package com.chinanetcenter.streampusherdemo.activity;

import java.util.HashMap;
import java.util.Map;

import cnc.cad.validsdk.ValidListener;
import cnc.cad.validsdk.ValidParam;
import cnc.cad.validsdk.ValidSdk;

import com.chinanetcenter.StreamPusher.utils.ALog;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.utils.DialogUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnDismissListener {

    private static final String TAG = "LoginActivity";

    private EditText mAppIdET = null;
    private EditText mAuthKeydET = null;
    private ImageButton mClearAppIdBt = null;
    private ImageButton mClearAuthKeyBt = null;
    private Button mLoginBt = null;

    private ProgressDialog mProgressDialog = null;

    private String mAppIdStr = "";
    private String mAuthKeyStr = "";

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btn_appid_clear:
                mAppIdET.setText("");
                break;
            case R.id.btn_authkey_clear:
                mAuthKeydET.setText("");
                break;
            case R.id.btn_login:
                actionLogin();
                break;
            default:
                break;
            }

        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mAppIdET.hasFocus()) {
                mClearAppIdBt.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
            }
            if (mAuthKeydET.hasFocus()) {
                mClearAuthKeyBt.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private ValidListener mValidListener = new ValidListener() {

        @Override
        public void onComplete(int code, String msg) {
            Log.d(TAG, "onComplete ---   " + code + "," + msg);
            if (code == 1) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        persistUserInfo();
                        if (mProgressDialog != null) {
                            mProgressDialog.cancel();
                            mProgressDialog = null;
                        }
                        Toast.makeText(getApplicationContext(), "鉴权成功", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(LoginActivity.this, ConfigActivity.class);
                        LoginActivity.this.startActivity(intent);
                        LoginActivity.this.finish();
                    }
                });

            }
        }

        public void onInfo(int code, String msg) {
            Log.d(TAG, "onInfo ----" + code + "," + msg);
        }

        @Override
        public void onError(int code, final String msg) {
            Log.d(TAG, " onError------ " + code + "," + msg);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.cancel();
                        mProgressDialog = null;
                    }
                    DialogUtils.showAlertDialog(LoginActivity.this, "鉴权失败：" + msg);
                }
            });

        }
    };

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initLayout();
    }

    private void initLayout() {
        mAppIdET = (EditText) findViewById(R.id.et_appid);
        mAppIdET.addTextChangedListener(mTextWatcher);
        mAuthKeydET = (EditText) findViewById(R.id.et_authkey);
        mAuthKeydET.addTextChangedListener(mTextWatcher);
        mClearAppIdBt = (ImageButton) findViewById(R.id.btn_appid_clear);
        mClearAppIdBt.setOnClickListener(mClickListener);
        mClearAuthKeyBt = (ImageButton) findViewById(R.id.btn_authkey_clear);
        mClearAuthKeyBt.setOnClickListener(mClickListener);
        mLoginBt = (Button) findViewById(R.id.btn_login);
        mLoginBt.setOnClickListener(mClickListener);

        Map<String, String> configs = inflateExitsUserInfo();
        mAppIdET.setText(configs.get("appId"));
        mClearAppIdBt.setVisibility(TextUtils.isEmpty(mAppIdET.getText().toString()) ? View.INVISIBLE : View.VISIBLE);
        mAuthKeydET.setText(configs.get("authKey"));
        mClearAuthKeyBt.setVisibility(TextUtils.isEmpty(mAuthKeydET.getText().toString()) ? View.INVISIBLE : View.VISIBLE);

    }

    private void actionLogin() {

        String appId = mAppIdET.getText().toString();
        String authKey = mAuthKeydET.getText().toString();
        if (TextUtils.isEmpty(appId)) {
            DialogUtils.showAlertDialog(this, "请输入您的鉴权appid");
            return;
        }
        if (TextUtils.isEmpty(authKey)) {
            DialogUtils.showAlertDialog(this, "请输入您的鉴权密钥");
            return;
        }

        ValidParam validParam = generateValidParam("STREAMER_SDK", appId, authKey);
        mAppIdStr = appId;
        mAuthKeyStr = authKey;
        if (validParam != null) {
            ValidSdk.valid(validParam, getApplicationContext(), mValidListener, 1, true);
        }
        if (mProgressDialog == null)
            mProgressDialog = DialogUtils.showSimpleProgressDialog(this, "正在进行联网鉴权，请稍等", true, this);

    }

    private Map<String, String> inflateExitsUserInfo() {
        String defaultAppId = getString("appId");
        String defaultAuthKey = getString("authKey");
        Map<String, String> configs = new HashMap<String, String>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appId = preferences.getString("appId", defaultAppId);
        String authKey = preferences.getString("authKey", defaultAuthKey);
        configs.put("appId", appId);
        configs.put("authKey", authKey);
        return configs;
    }

    private boolean persistUserInfo() {
        return PreferenceManager.getDefaultSharedPreferences(this).edit().putString("appId", mAppIdStr)
                .putString("authKey", mAuthKeyStr).commit();
    }

    private ValidParam generateValidParam(String name, String appId, String authKey) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(appId) || TextUtils.isEmpty(authKey)) {
            ALog.e(TAG, "authenticate param is empty, you must provider all of them.");
            return null;
        }
        return new ValidParam(name, 1, appId, authKey);
    }

    private String getString(String name) {
        int id = getResources().getIdentifier(name, "string", getPackageName());
        String result = "";
        if (id != 0) {
            try {
                result = getResources().getString(id);
            } catch (NotFoundException e) {
            }
        }
        return result;
    }

}
