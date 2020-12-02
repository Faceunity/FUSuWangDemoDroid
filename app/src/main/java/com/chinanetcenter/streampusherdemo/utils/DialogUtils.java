package com.chinanetcenter.streampusherdemo.utils;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chinanetcenter.StreamPusher.sdk.SPManager;
import com.chinanetcenter.StreamPusher.sdk.SPVideoFilter;
import com.chinanetcenter.streampusherdemo.R;
import com.chinanetcenter.streampusherdemo.filter.VideoFilterDemo1;
import com.chinanetcenter.streampusherdemo.filter.VideoFilterDemo2;
import com.chinanetcenter.streampusherdemo.filter.VideoFilterDemo3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class DialogUtils {

    public interface InputConfigClickListener {
        void onResult(DialogInterface dialog, HashMap<String, String> result);
    }

    /**
     * 
     * @param context
     * @param keys
     * @param defaultValues
     * @param promptStrings
     * @param configResultListhener
     * @return
     */
    public static Dialog showConfigInputDialog(final Context context, int inputType, final String[] keys, String[] defaultValues, String[] promptStrings, final InputConfigClickListener configResultListhener) {
        if (keys == null || keys.length == 0)
            return null;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout container = new LinearLayout(context);
        int paddingPix = dpToPx(24, context.getResources());
        int editTextHeight = dpToPx(12, context.getResources());
        container.setPadding(paddingPix, paddingPix, paddingPix, paddingPix);
        container.setOrientation(LinearLayout.VERTICAL);

        final List<EditText> evList = new ArrayList<EditText>(keys.length);

        for (int i = 0; i < keys.length; i++) {
            LinearLayout item = (LinearLayout) inflater.inflate(R.layout.eidt_dialog_item, null);
            String textString = keys[i];
            if (promptStrings != null && i < promptStrings.length) {
                textString = promptStrings[i];
            }
            TextView textView = (TextView) item.findViewById(R.id.text_view);
            EditText editText = (EditText) item.findViewById(R.id.edit_text);
            textView.setText(textString);
            if (defaultValues != null && i < defaultValues.length) {
                editText.setText(defaultValues[i]);
                editText.setSelection(defaultValues[i].length());
                editText.setInputType(inputType);
            }

            // add view
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            container.addView(item, lp);

            // save view
            evList.add(editText);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(container, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        OnClickListener onClick = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    HashMap<String, String> result = new HashMap<String, String>();
                    for (int i = 0; i < evList.size(); i++) {
                        result.put(keys[i], evList.get(i).getText().toString());
                    }
                    configResultListhener.onResult(dialog, result);
                case AlertDialog.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
                }

            }
        };

        builder.setView(scrollView);
        builder.setPositiveButton(android.R.string.ok, onClick);
        builder.setNegativeButton(android.R.string.cancel, onClick);

        Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static AlertDialog showRtmpUrlInputDialog(final Context context, DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout container = new LinearLayout(context);
        int paddingPix = dpToPx(24, context.getResources());
        container.setPadding(paddingPix, paddingPix, paddingPix, paddingPix);
        container.setOrientation(LinearLayout.VERTICAL);

        EditText editText = new EditText(context);
        editText.setId(android.R.id.edit);
        editText.setMaxLines(3);
        editText.setMinLines(1);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(true);

        // add view
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        container.addView(editText, lp);

        // save view
        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, onClickListener);
        builder.setNegativeButton(android.R.string.cancel, onClickListener);

        builder.setTitle("请输入有效的推流地址");

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    public static Dialog showAlertDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static Dialog showYNDialog(Context context, String message, String yesBtn, String noBtn, OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(message);
        builder.setPositiveButton(yesBtn, listener);
        builder.setNegativeButton(noBtn, listener);
        builder.setCancelable(true);
        Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showSimpleProgressDialog(Context context, String message, boolean cancelable, DialogInterface.OnDismissListener dismissListener) {
        String cancleStr = context.getResources().getString(android.R.string.cancel);
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(android.R.string.dialog_alert_title);
        dialog.setMessage(message);
        if (cancelable) {
            dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, cancleStr, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        }
        dialog.setOnDismissListener(dismissListener);
        dialog.show();
        return dialog;
    }

    public static Dialog showSingleChoiceDialog(Context context, String title, String[] singleChoiceItems, int checkedItem, final DialogInterface.OnClickListener onClickListener) {
        if (context == null)
            return null;
        AlertDialog alertDialog = new AlertDialog.Builder(context).setSingleChoiceItems(singleChoiceItems, checkedItem, new OnClickListener() {

            @Override
            public void onClick(DialogInterface alertDialog, int which) {
                if (onClickListener != null) {
                    onClickListener.onClick(alertDialog, which);
                    alertDialog.dismiss();
                }

            }
        }).setTitle(title).setCancelable(true).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        return alertDialog;
    }

    public static Dialog showMultiChoiceDialog(Context context, String title, CharSequence[] Items, boolean[] checkedItem, final DialogInterface.OnMultiChoiceClickListener onMultiClickListener, final DialogInterface.OnClickListener onClickListener) {
        if (context == null)
            return null;
        AlertDialog alertDialog = new AlertDialog.Builder(context).setMultiChoiceItems(Items, checkedItem, new OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface alertDialog, int which, boolean isChecked) {
                if (onMultiClickListener != null) {
                	onMultiClickListener.onClick(alertDialog, which, isChecked);
                }
            }
        }).setTitle(title).setCancelable(true).setPositiveButton(android.R.string.ok, onClickListener).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
        return alertDialog;
    }

    public static AlertDialog showSingleInputNumberDialog(final Context context, String title, String prompt, String defaultText, DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout container = new LinearLayout(context);
        int paddingPix = dpToPx(24, context.getResources());
        container.setPadding(paddingPix, paddingPix, paddingPix, paddingPix);
        container.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.eidt_dialog_item, null);
        TextView textView = (TextView) item.findViewById(R.id.text_view);
        textView.setText(prompt);
        EditText editText = (EditText) item.findViewById(R.id.edit_text);
        editText.setFocusable(false);
        editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        editText.setWidth(dpToPx(50, context.getResources()));
        editText.setFocusableInTouchMode(true);
        editText.setText(defaultText);

        // add view
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        container.addView(item, lp);

        // save view
        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, onClickListener);
        builder.setNegativeButton(android.R.string.cancel, onClickListener);

        builder.setTitle(title);

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    public static AlertDialog showSingleInputTextDialog(final Context context, String title, String prompt, String defaultText, DialogInterface.OnClickListener onClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout container = new LinearLayout(context);
        int paddingPix = dpToPx(24, context.getResources());
        container.setPadding(paddingPix, paddingPix, paddingPix, paddingPix);
        container.setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.eidt_dialog_item, null);
        TextView textView = (TextView) item.findViewById(R.id.text_view);
        textView.setText(prompt);
        EditText editText = (EditText) item.findViewById(R.id.edit_text);
        editText.setFocusable(false);
        editText.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        editText.setWidth(dpToPx(50, context.getResources()));
        editText.setFocusableInTouchMode(true);
        editText.setText(defaultText);

        // add view
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        container.addView(item, lp);

        // save view
        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, onClickListener);
        builder.setNegativeButton(android.R.string.cancel, onClickListener);

        builder.setTitle(title);

        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }
    
    public static Dialog showVolumeAdjustDialog(Context context, int micProgress, int bgmProgress , SeekBar.OnSeekBarChangeListener listerner) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.bgm_volume_dialog_item, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        SeekBar micVolumeseekBar = (SeekBar) rootView.findViewById(R.id.sb_mic_volume);
        micVolumeseekBar.setMax(20);
        micVolumeseekBar.setProgress(micProgress);
        micVolumeseekBar.setOnSeekBarChangeListener(listerner);
        SeekBar bgmVolumeseekBar = (SeekBar) rootView.findViewById(R.id.sb_bgm_volume);
        bgmVolumeseekBar.setMax(20);
        bgmVolumeseekBar.setProgress(bgmProgress);
        bgmVolumeseekBar.setOnSeekBarChangeListener(listerner);
        builder.setView(rootView);
        Dialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    private static SPManager.FilterType mCurrentFilter = SPManager.FilterType.NONE;
    private static int mCurrenStyleFilter = 0;
    private static int mCurrenCustomFilter = 0;
    private static int mStyleLevel = 5;
    private static LifecycleObserver beautyDialogLifecycleObserver;

    public static Dialog showBeautyPickDialog(final FragmentActivity context) {
        if(beautyDialogLifecycleObserver == null) {
            beautyDialogLifecycleObserver = new LifecycleObserver() {

                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                protected void onCreate() {
                    //将DialogUtils中记录的标志位置为默认
                    mCurrenStyleFilter = 0;
                    mCurrenCustomFilter = 0;
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                protected void onDestroy() {
                    beautyDialogLifecycleObserver = null;
                }

            };
            context.getLifecycle().addObserver(beautyDialogLifecycleObserver);
        }

        if(mCurrenCustomFilter == 3) {
            CommonUtils.showThirdPartFilterDialog(context);
            return null;
        }

        mCurrentFilter = SPManager.getPushState().filter;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Translucent_Diglog);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rootView = inflater.inflate(R.layout.beauty_pick_view, null);

        builder.setView(rootView);
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));

        final SeekBar beautySeekBar = (SeekBar) rootView.findViewById(R.id.beauty_level);
        final TextView beautyValueTv = (TextView) rootView.findViewById(R.id.beauty_level_value);
        final SeekBar styleSeekBar = (SeekBar) rootView.findViewById(R.id.style_level);
        final TextView styleValueTv = (TextView) rootView.findViewById(R.id.style_level_value);
        beautySeekBar.setMax(10);//美颜参数范围0~10
        if(mCurrentFilter.getLevel() < 0) {
            beautySeekBar.setEnabled(false);
            beautySeekBar.setProgress(0);
            beautyValueTv.setText("" + 0);
        }else {
            beautySeekBar.setEnabled(true);
            beautySeekBar.setProgress(mCurrentFilter.getLevel());
            beautyValueTv.setText("" + mCurrentFilter.getLevel());
        }
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                switch (seekBar.getId()) {
                case R.id.beauty_level:
                    if (progress != mCurrentFilter.getLevel()) {
                        mCurrentFilter.setLevel(progress);
                        beautyValueTv.setText("" + progress);
                        SPManager.switchFilter(mCurrentFilter);
                    }
                    break;
                case R.id.style_level:
                    String modelPath = context.getResources().getStringArray(R.array.filter_path)[mCurrenStyleFilter];
                    if (progress != mStyleLevel && SPManager.setStyleFilterModel(modelPath, progress)) {
                        mStyleLevel = progress;
                        styleValueTv.setText("" + mStyleLevel);
                    }
                    break;

                default:
                    break;
                }
            }
        };
        beautySeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        styleSeekBar.setMax(10);//滤镜参数范围0~10
        styleSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch (parent.getId()) {
                    case R.id.beauty_type:
                        if (SPManager.switchFilter(SPManager.FilterType.values()[pos])) {
                            Toast.makeText(context, SPManager.FILTER_TYPE_ALL[pos].toString(), Toast.LENGTH_SHORT).show();
                            mCurrentFilter = SPManager.FilterType.values()[pos];
                            if (mCurrentFilter.getLevel() < 0) {
                                beautySeekBar.setEnabled(false);
                                beautySeekBar.setProgress(0);
                                beautyValueTv.setText("" + 0);
                            } else {
                                beautySeekBar.setEnabled(true);
                                beautySeekBar.setProgress(mCurrentFilter.getLevel());
                                beautyValueTv.setText("" + mCurrentFilter.getLevel());
                            }
                        }
                        break;
                    case R.id.style_type:
                        String modelPath = context.getResources().getStringArray(R.array.filter_path)[pos];
                        if (SPManager.setStyleFilterModel(modelPath, mStyleLevel)) {
                            mCurrenStyleFilter = pos;
                            int[] disableIdxs = context.getResources().getIntArray(R.array.filter_index_unsupport_level);
                            boolean enable = true;
                            for (int i = 0; i < disableIdxs.length; i++) {
                                if (pos == disableIdxs[i]) {
                                    enable = false;
                                }
                            }
                            styleSeekBar.setEnabled(enable);
                            styleSeekBar.setProgress(enable ? mStyleLevel : 0);
                            styleValueTv.setText(enable ? "" + mStyleLevel : "0");
                        }
                        break;
                    case R.id.custom_type:
                        switch (pos) {
                            case 0://无
                                //设置null或list为空可关闭自定义滤镜
                                SPManager.setFilter((SPVideoFilter) null);
                                break;
                            case 1://自定义滤镜
                                //初始化自定义
                                SPManager.setFilter(new VideoFilterDemo1());
                                break;
                            case 2://自定义滤镜组
                                List<SPVideoFilter> filters = new ArrayList<SPVideoFilter>();
                                filters.add(new VideoFilterDemo2());
                                filters.add(new VideoFilterDemo3());
                                SPManager.setFilter(filters);
                                break;
                            case 3://faceu滤镜
                                dialog.dismiss();
                                CommonUtils.showThirdPartFilterDialog(context);
                                break;
                        }
                        mCurrenCustomFilter = pos;
                        break;

                    default:
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        };
        
        Spinner beautySpinner = (Spinner) rootView.findViewById(R.id.beauty_type);
        String filterNames[] = new String[SPManager.FILTER_TYPE_ALL.length];
        for (int i = 0; i < SPManager.FILTER_TYPE_ALL.length; i++) {
            filterNames[i] = SPManager.FILTER_TYPE_ALL[i].toString();
        }
        beautySpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, filterNames));
        beautySpinner.setOnItemSelectedListener(onItemSelectedListener);
        beautySpinner.setSelection(mCurrentFilter.ordinal());
        Spinner styleSpinner = (Spinner) rootView.findViewById(R.id.style_type);
        styleSpinner.setOnItemSelectedListener(onItemSelectedListener);
        styleSpinner.setSelection(mCurrenStyleFilter);
        Spinner customFilterSpinner = (Spinner) rootView.findViewById(R.id.custom_type);
        customFilterSpinner.setOnItemSelectedListener(onItemSelectedListener);
        ArrayList<String> nameList = new ArrayList<>();
        nameList.add("无");//0
        nameList.add("滤镜");//1
        nameList.add("滤镜组");//2
       if(Config.getVersion() == Config.VERSION_FACEU) {
            nameList.add("faceu滤镜");//3
        }
        customFilterSpinner.setAdapter(new ArrayAdapter<Object>(context, android.R.layout.simple_spinner_dropdown_item, nameList.toArray()));
        customFilterSpinner.setSelection(mCurrenCustomFilter);

        dialog.show();
        return dialog;
    }

}
