package com.xgimirom.presets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class SettingsActivity extends Activity {
    static final String PREFS_NAME = "keystone_presets";
    static final String PREF_CONFIRM_SAVE = "settings.confirm_save";
    static final String PREF_CONFIRM_APPLY = "settings.confirm_apply";
    private static final int SLOT_COUNT = 6;
    private static final int COLUMNS = 3;
    private static final int COLOR_BG_TOP = Color.rgb(13, 17, 24);
    private static final int COLOR_BG_BOTTOM = Color.rgb(21, 27, 38);
    private static final int COLOR_CARD = Color.rgb(29, 36, 48);
    private static final int COLOR_CARD_BORDER = Color.rgb(55, 66, 83);
    private static final int COLOR_BLUE = Color.rgb(72, 132, 255);
    private static final int COLOR_MUTED = Color.rgb(153, 164, 181);

    private final TextView[] nameValueViews = new TextView[SLOT_COUNT];
    private SharedPreferences preferences;
    private Switch saveConfirmationSwitch;
    private Switch applyConfirmationSwitch;

    static String slotNameKey(int slot) {
        return "slot." + slot + ".name";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setContentView(buildContentView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSettings();
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(24), dp(48), dp(16));
        root.setBackground(verticalGradient(COLOR_BG_TOP, COLOR_BG_BOTTOM));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, matchWrap());

        Button back = makeButton("返回", false);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(104), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleParams.setMarginStart(dp(16));
        header.addView(titles, titleParams);

        TextView title = makeText("预设设置", 28, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(title);
        titles.addView(makeText("名称与操作确认", 13, COLOR_MUTED), topMargin(dp(3)));

        TextView version = makeText("v0.9.1", 14, Color.rgb(192, 211, 255));
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(16), dp(8), dp(16), dp(8));
        version.setBackground(roundRect(Color.rgb(34, 52, 82), dp(18), 0, 0));
        header.addView(version);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.topMargin = dp(16);
        root.addView(scroll, scrollParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, dp(16));
        scroll.addView(content, matchWrap());

        TextView notice = makeText(
                "所有设置只保存在本机。关闭确认后，按下按钮会立即执行操作。",
                14, Color.rgb(184, 196, 213));
        notice.setPadding(dp(16), dp(12), dp(16), dp(12));
        notice.setBackground(roundRect(Color.rgb(25, 34, 47), dp(12),
                Color.rgb(46, 58, 75), dp(1)));
        content.addView(notice, matchWrap());

        content.addView(sectionTitle("操作确认"), topMargin(dp(16)));
        LinearLayout confirmRow = new LinearLayout(this);
        confirmRow.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(confirmRow, topMargin(dp(8)));

        confirmRow.addView(buildToggleCard(
                "保存前确认", "覆盖或新建预设前显示确认界面", PREF_CONFIRM_SAVE, true),
                weightedCardParams(true));
        confirmRow.addView(buildToggleCard(
                "应用前确认", "改变当前投影画面前显示确认界面", PREF_CONFIRM_APPLY, false),
                weightedCardParams(false));

        content.addView(sectionTitle("预设名称"), topMargin(dp(16)));
        for (int row = 0; row < 2; row++) {
            LinearLayout nameRow = new LinearLayout(this);
            nameRow.setOrientation(LinearLayout.HORIZONTAL);
            content.addView(nameRow, topMargin(dp(row == 0 ? 8 : 10)));
            for (int column = 0; column < COLUMNS; column++) {
                int slot = row * COLUMNS + column + 1;
                nameRow.addView(buildNameCard(slot), weightedNameParams(column));
            }
        }

        Button reset = makeButton("恢复默认设置", false);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmReset();
            }
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(dp(192), dp(48));
        resetParams.topMargin = dp(16);
        content.addView(reset, resetParams);

        back.requestFocus();
        return root;
    }

    private View buildToggleCard(String title, String summary, final String key,
            boolean saveToggle) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20), dp(12), dp(20), dp(12));
        card.setFocusable(true);
        card.setClickable(true);
        card.setBackground(focusableCardBackground());

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        card.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView titleView = makeText(title, 16, Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        copy.addView(titleView);
        copy.addView(makeText(summary, 14, COLOR_MUTED), topMargin(dp(4)));

        Switch toggle = new Switch(this);
        toggle.setTextSize(14);
        toggle.setTextColor(Color.WHITE);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setShowText(true);
        toggle.setTextOn("开启");
        toggle.setTextOff("关闭");
        toggle.setFocusable(false);
        toggle.setClickable(false);
        toggle.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(key, checked).apply());
        card.addView(toggle, new LinearLayout.LayoutParams(dp(80), dp(48)));
        card.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));
        if (saveToggle) {
            saveConfirmationSwitch = toggle;
        } else {
            applyConfirmationSwitch = toggle;
        }
        return card;
    }

    private View buildNameCard(final int slot) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(8), dp(12), dp(8));
        card.setBackground(roundRect(COLOR_CARD, dp(13), COLOR_CARD_BORDER, dp(1)));

        TextView number = makeText(String.format(Locale.US, "%02d", slot), 14,
                Color.rgb(207, 220, 255));
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        number.setBackground(roundRect(Color.rgb(47, 66, 103), dp(8), 0, 0));
        card.addView(number, new LinearLayout.LayoutParams(dp(38), dp(30)));

        TextView name = makeText(defaultName(slot), 16, Color.WHITE);
        name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        nameParams.setMarginStart(dp(12));
        card.addView(name, nameParams);
        nameValueViews[slot - 1] = name;

        Button edit = makeButton("修改", false);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRenameDialog(slot);
            }
        });
        card.addView(edit, new LinearLayout.LayoutParams(dp(80), dp(44)));
        return card;
    }

    private void showRenameDialog(final int slot) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(getSlotName(slot));
        input.setSelectAllOnFocus(true);
        input.setHint(defaultName(slot));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        input.setPadding(dp(20), dp(8), dp(20), dp(8));

        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(24), 0, dp(24), 0);
        holder.addView(input, matchWrap());

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改预设 " + slot + " 名称")
                .setMessage("最多 14 个字符；留空将恢复默认名称。")
                .setView(holder)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存名称", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String value = input.getText().toString().trim();
                SharedPreferences.Editor editor = preferences.edit();
                if (value.length() == 0 || defaultName(slot).equals(value)) {
                    editor.remove(slotNameKey(slot));
                } else {
                    editor.putString(slotNameKey(slot), value);
                }
                editor.apply();
                refreshName(slot);
                dialog.dismiss();
                Toast.makeText(this, "名称已更新", Toast.LENGTH_SHORT).show();
            });
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            InputMethodManager keyboard = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) {
                keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        dialog.show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("恢复默认设置？")
                .setMessage("六个预设名称将恢复默认，保存和应用确认将重新开启。"
                        + " 已保存的校正数据不会删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("恢复默认", (dialog, which) -> resetSettings())
                .show();
    }

    private void resetSettings() {
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(PREF_CONFIRM_SAVE, true)
                .putBoolean(PREF_CONFIRM_APPLY, true);
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            editor.remove(slotNameKey(slot));
        }
        editor.apply();
        refreshSettings();
        Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
    }

    private void refreshSettings() {
        if (saveConfirmationSwitch != null) {
            saveConfirmationSwitch.setChecked(
                    preferences.getBoolean(PREF_CONFIRM_SAVE, true));
        }
        if (applyConfirmationSwitch != null) {
            applyConfirmationSwitch.setChecked(
                    preferences.getBoolean(PREF_CONFIRM_APPLY, true));
        }
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            refreshName(slot);
        }
    }

    private void refreshName(int slot) {
        if (nameValueViews[slot - 1] != null) {
            nameValueViews[slot - 1].setText(getSlotName(slot));
        }
    }

    private String getSlotName(int slot) {
        return preferences.getString(slotNameKey(slot), defaultName(slot));
    }

    private String defaultName(int slot) {
        return "预设 " + slot;
    }

    private TextView sectionTitle(String text) {
        TextView title = makeText(text, 16, Color.rgb(213, 222, 238));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    private LinearLayout.LayoutParams weightedCardParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(80), 1.0f);
        if (!first) {
            params.setMarginStart(dp(16));
        }
        return params;
    }

    private LinearLayout.LayoutParams weightedNameParams(int column) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(64), 1.0f);
        if (column > 0) {
            params.setMarginStart(dp(12));
        }
        return params;
    }

    private Button makeButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(buttonBackground(primary));
        button.setTextColor(buttonTextColors(primary));
        button.setOnFocusChangeListener((view, focused) -> {
            view.animate().scaleX(focused ? 1.035f : 1.0f)
                    .scaleY(focused ? 1.035f : 1.0f).setDuration(120L).start();
            view.setElevation(dp(focused ? 10 : 1));
        });
        return button;
    }

    private StateListDrawable buttonBackground(boolean primary) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                roundRect(Color.rgb(238, 244, 255), dp(11), Color.WHITE, dp(2)));
        states.addState(new int[]{android.R.attr.state_pressed},
                roundRect(primary ? Color.rgb(42, 94, 207) : Color.rgb(62, 72, 89),
                        dp(11), 0, 0));
        states.addState(new int[]{},
                roundRect(primary ? Color.rgb(42, 94, 207) : Color.rgb(48, 57, 72),
                        dp(11), primary ? COLOR_BLUE : Color.rgb(74, 85, 103), dp(1)));
        return states;
    }

    private StateListDrawable focusableCardBackground() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                roundRect(Color.rgb(42, 55, 75), dp(13), COLOR_BLUE, dp(2)));
        states.addState(new int[]{},
                roundRect(COLOR_CARD, dp(13), COLOR_CARD_BORDER, dp(1)));
        return states;
    }

    private ColorStateList buttonTextColors(boolean primary) {
        return new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{Color.rgb(24, 37, 61), primary ? Color.WHITE
                        : Color.rgb(225, 231, 241)});
    }

    private TextView makeText(String text, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = margin;
        return params;
    }

    private GradientDrawable verticalGradient(int top, int bottom) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor,
            int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
