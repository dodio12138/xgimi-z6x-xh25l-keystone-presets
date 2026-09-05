package com.xgimirom.presets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity
        implements XgimiEnvironmentBridge.Listener {
    private static final String PREFS_NAME = "keystone_presets";
    private static final int SLOT_COUNT = 6;
    private static final int COLUMNS = 3;
    private static final int COLOR_BG_TOP = Color.rgb(13, 17, 24);
    private static final int COLOR_BG_BOTTOM = Color.rgb(21, 27, 38);
    private static final int COLOR_CARD = Color.rgb(29, 36, 48);
    private static final int COLOR_CARD_BORDER = Color.rgb(55, 66, 83);
    private static final int COLOR_BLUE = Color.rgb(72, 132, 255);
    private static final int COLOR_BLUE_DARK = Color.rgb(42, 94, 207);
    private static final int COLOR_GREEN = Color.rgb(83, 210, 149);
    private static final int COLOR_AMBER = Color.rgb(255, 190, 92);
    private static final int COLOR_MUTED = Color.rgb(153, 164, 181);
    private static final String[] ENV_KEYS = {
        "shape_type",
        "zoom_factor",
        "kst_ofs_backup",
        "kst_ofs_limit_backup",
        "kst_ofs_limit",
        "kst_ofs"
    };

    private final Button[] actionButtons = new Button[SLOT_COUNT * 2];
    private final TextView[] slotNameViews = new TextView[SLOT_COUNT];
    private final TextView[] slotStateViews = new TextView[SLOT_COUNT];
    private final TextView[] slotTimeViews = new TextView[SLOT_COUNT];
    private final GmpfKeystoneBridge gmpfBridge = new GmpfKeystoneBridge();
    private XgimiEnvironmentBridge bridge;
    private SharedPreferences preferences;
    private TextView statusView;
    private Button settingsButton;
    private boolean serviceConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bridge = new XgimiEnvironmentBridge(this, this);
        setContentView(buildContentView());
        configureFocusNavigation();
        refreshAllSlots();
        setActionsEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setStatus("●  正在连接极米显示服务…", COLOR_BLUE);
        bridge.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            refreshAllSlots();
            setActionsEnabled(serviceConnected);
        }
    }

    @Override
    protected void onStop() {
        bridge.disconnect();
        serviceConnected = false;
        super.onStop();
    }

    @Override
    public void onConnectionChanged(final boolean connected, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceConnected = connected;
                setStatus(
                        connected ? "●  设备已连接，可以保存或应用预设" : "●  " + message,
                        connected ? COLOR_GREEN : Color.rgb(255, 112, 112));
                setActionsEnabled(connected);
                if (connected && actionButtons[0] != null) {
                    actionButtons[0].requestFocus();
                }
            }
        });
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(24), dp(48), dp(16));
        root.setBackground(verticalGradient(COLOR_BG_TOP, COLOR_BG_BOTTOM, 0));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, matchWrap());

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        header.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = makeText("投影校正预设", 28, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(title);

        TextView subtitle = makeText("Z6X · XH25L", 14, COLOR_MUTED);
        titles.addView(subtitle, topMargin(dp(4)));

        TextView version = makeText("v0.9.1", 14, Color.rgb(192, 211, 255));
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(16), dp(8), dp(16), dp(8));
        version.setBackground(roundRect(Color.rgb(34, 52, 82), dp(18), 0, 0));
        header.addView(version);

        settingsButton = makeButton("设置", false);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                dp(92), dp(48));
        settingsParams.setMarginStart(dp(12));
        header.addView(settingsButton, settingsParams);

        statusView = makeText("●  尚未连接", 16, COLOR_BLUE);
        statusView.setPadding(dp(18), dp(13), dp(18), dp(13));
        statusView.setBackground(roundRect(Color.rgb(25, 34, 47), dp(12),
                Color.rgb(46, 58, 75), dp(1)));
        root.addView(statusView, topMargin(dp(16)));

        for (int row = 0; row < 2; row++) {
            LinearLayout cardRow = new LinearLayout(this);
            cardRow.setOrientation(LinearLayout.HORIZONTAL);
            cardRow.setBaselineAligned(false);
            LinearLayout.LayoutParams rowParams = topMargin(dp(row == 0 ? 16 : 12));
            root.addView(cardRow, rowParams);
            for (int column = 0; column < COLUMNS; column++) {
                int slot = row * COLUMNS + column + 1;
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                if (column < COLUMNS - 1) {
                    cardParams.setMarginEnd(dp(16));
                }
                cardRow.addView(buildSlotCard(slot), cardParams);
            }
        }

        return root;
    }

    private View buildSlotCard(final int slot) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        card.setBackground(roundRect(COLOR_CARD, dp(16), COLOR_CARD_BORDER, dp(1)));
        card.setElevation(dp(3));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, matchWrap());

        TextView number = makeText(String.format(Locale.US, "%02d", slot), 14,
                Color.rgb(207, 220, 255));
        number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        number.setBackground(roundRect(Color.rgb(47, 66, 103), dp(8), 0, 0));
        top.addView(number, new LinearLayout.LayoutParams(dp(38), dp(30)));

        TextView label = makeText("预设 " + slot, 20, Color.WHITE);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        labelParams.setMarginStart(dp(12));
        top.addView(label, labelParams);
        slotNameViews[slot - 1] = label;

        TextView state = makeText("尚未保存", 14, COLOR_MUTED);
        state.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(state);
        slotStateViews[slot - 1] = state;

        TextView time = makeText("可保存当前校正数据", 14, COLOR_MUTED);
        card.addView(time, topMargin(dp(8)));
        slotTimeViews[slot - 1] = time;

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(actions, topMargin(dp(16)));

        Button save = makeButton("保存当前", false);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestSaveSlot(slot);
            }
        });
        actions.addView(save, new LinearLayout.LayoutParams(
                0, dp(56), 1.0f));

        Button restore = makeButton("应用预设", true);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestRestoreSlot(slot);
            }
        });
        LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(
                0, dp(56), 1.0f);
        restoreParams.setMarginStart(dp(12));
        actions.addView(restore, restoreParams);

        int offset = (slot - 1) * 2;
        actionButtons[offset] = save;
        actionButtons[offset + 1] = restore;
        return card;
    }

    private Button makeButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(buttonBackground(primary));
        button.setTextColor(buttonTextColors(primary));
        button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                view.animate()
                        .scaleX(focused ? 1.035f : 1.0f)
                        .scaleY(focused ? 1.035f : 1.0f)
                        .setDuration(120L)
                        .start();
                view.setElevation(dp(focused ? 10 : 1));
            }
        });
        return button;
    }

    private StateListDrawable buttonBackground(boolean primary) {
        StateListDrawable states = new StateListDrawable();
        states.addState(
                new int[]{android.R.attr.state_focused},
                roundRect(Color.rgb(238, 244, 255), dp(11), Color.WHITE, dp(2)));
        states.addState(
                new int[]{-android.R.attr.state_enabled},
                roundRect(Color.rgb(37, 43, 54), dp(11), 0, 0));
        states.addState(
                new int[]{android.R.attr.state_pressed},
                roundRect(primary ? COLOR_BLUE_DARK : Color.rgb(62, 72, 89),
                        dp(11), 0, 0));
        states.addState(
                new int[]{},
                roundRect(primary ? COLOR_BLUE_DARK : Color.rgb(48, 57, 72),
                        dp(11), primary ? COLOR_BLUE : Color.rgb(74, 85, 103), dp(1)));
        return states;
    }

    private ColorStateList buttonTextColors(boolean primary) {
        return new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_focused},
                    new int[]{-android.R.attr.state_enabled},
                    new int[]{}
                },
                new int[]{
                    Color.rgb(24, 37, 61),
                    Color.rgb(94, 103, 117),
                    primary ? Color.WHITE : Color.rgb(225, 231, 241)
                });
    }

    private void configureFocusNavigation() {
        if (settingsButton != null && actionButtons[0] != null) {
            settingsButton.setNextFocusDownId(actionButtons[0].getId());
        }
        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            Button save = actionButtons[slotIndex * 2];
            Button restore = actionButtons[slotIndex * 2 + 1];
            int previousSlot = Math.max(0, slotIndex - 1);
            int nextSlot = Math.min(SLOT_COUNT - 1, slotIndex + 1);
            int aboveSlot = slotIndex >= COLUMNS ? slotIndex - COLUMNS : slotIndex;
            int belowSlot = slotIndex + COLUMNS < SLOT_COUNT
                    ? slotIndex + COLUMNS : slotIndex;

            Button previousRestore = actionButtons[previousSlot * 2 + 1];
            Button aboveRestore = actionButtons[aboveSlot * 2 + 1];
            Button belowRestore = actionButtons[belowSlot * 2 + 1];

            save.setNextFocusLeftId(previousRestore.isEnabled()
                    ? previousRestore.getId() : actionButtons[previousSlot * 2].getId());
            save.setNextFocusRightId(restore.isEnabled()
                    ? restore.getId() : actionButtons[nextSlot * 2].getId());
            restore.setNextFocusLeftId(save.getId());
            restore.setNextFocusRightId(actionButtons[nextSlot * 2].getId());
            save.setNextFocusUpId(slotIndex < COLUMNS && settingsButton != null
                    ? settingsButton.getId() : actionButtons[aboveSlot * 2].getId());
            save.setNextFocusDownId(actionButtons[belowSlot * 2].getId());
            restore.setNextFocusUpId(slotIndex < COLUMNS && settingsButton != null
                    ? settingsButton.getId() : (aboveRestore.isEnabled()
                    ? aboveRestore.getId() : actionButtons[aboveSlot * 2].getId()));
            restore.setNextFocusDownId(belowRestore.isEnabled()
                    ? belowRestore.getId() : actionButtons[belowSlot * 2].getId());
        }
    }

    private void requestSaveSlot(final int slot) {
        if (!preferences.getBoolean(SettingsActivity.PREF_CONFIRM_SAVE, true)) {
            saveSlot(slot);
            return;
        }
        String name = getSlotName(slot);
        String message = hasCompleteSlot(slot)
                ? "将用当前投影校正覆盖“" + name + "”中已有的数据。"
                : "将当前校正数据保存到“" + name + "”。";
        showConfirmation("确认保存当前校正？", message, "确认保存", new Runnable() {
            @Override
            public void run() {
                saveSlot(slot);
            }
        });
    }

    private void requestRestoreSlot(final int slot) {
        if (!preferences.getBoolean(SettingsActivity.PREF_CONFIRM_APPLY, true)) {
            restoreSlot(slot);
            return;
        }
        final String name = getSlotName(slot);
        showConfirmation(
                "确认应用“" + name + "”？",
                "将立即使用该预设调整当前投影画面。",
                "应用预设",
                new Runnable() {
                    @Override
                    public void run() {
                        restoreSlot(slot);
                    }
                });
    }

    private void showConfirmation(String title, String message, String positive,
            final Runnable action) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(32), dp(28), dp(32), dp(24));
        panel.setBackground(roundRect(COLOR_CARD, dp(16), COLOR_CARD_BORDER, dp(1)));

        TextView titleView = makeText(title, 24, Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        panel.addView(titleView, matchWrap());

        TextView messageView = makeText(message, 16, Color.rgb(194, 204, 220));
        messageView.setLineSpacing(0, 1.15f);
        panel.addView(messageView, topMargin(dp(16)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        panel.addView(actions, topMargin(dp(24)));

        Button cancel = makeButton("取消", false);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        actions.addView(cancel, new LinearLayout.LayoutParams(dp(128), dp(52)));

        Button confirm = makeButton(positive, true);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                action.run();
            }
        });
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                dp(152), dp(52));
        confirmParams.setMarginStart(dp(12));
        actions.addView(confirm, confirmParams);

        dialog.setContentView(panel);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = 0.72f;
            window.setAttributes(attributes);
            window.setLayout(dp(560), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        confirm.requestFocus();
    }

    private void saveSlot(int slot) {
        if (!bridge.isConnected()) {
            showMessage("系统服务尚未连接");
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        String primary = null;
        try {
            for (String key : ENV_KEYS) {
                String value = bridge.getEnvironment(key);
                boolean present = value != null;
                editor.putBoolean(presentKey(slot, key), present);
                if (present) {
                    editor.putString(valueKey(slot, key), value);
                } else {
                    editor.remove(valueKey(slot, key));
                }
                if ("kst_ofs".equals(key)) {
                    primary = value;
                }
            }
            if (primary == null || primary.length() == 0 || "0".equals(primary)) {
                setStatus("●  当前没有可保存的手动矫正数据", COLOR_AMBER);
                showMessage("请先完成一次手动矫正");
                return;
            }
            String fullCoordinates = gmpfBridge.capture();
            editor.putString("slot." + slot + ".gmpf_full", fullCoordinates);
            editor.putLong("slot." + slot + ".saved_at", System.currentTimeMillis());
            editor.apply();
            refreshSlot(slot);
            setActionsEnabled(serviceConnected);
            String name = getSlotName(slot);
            setStatus("●  “" + name + "”已保存校正数据", COLOR_GREEN);
            showMessage("已保存“" + name + "”");
        } catch (RemoteException error) {
            setStatus("●  保存失败：" + error.getMessage(), Color.rgb(255, 112, 112));
        } catch (RuntimeException error) {
            setStatus("●  保存校正数据失败：" + error.getMessage(),
                    Color.rgb(255, 112, 112));
        }
    }

    private void restoreSlot(int slot) {
        if (!bridge.isConnected()) {
            showMessage("系统服务尚未连接");
            return;
        }
        if (!hasCompleteSlot(slot)) {
            showMessage("“" + getSlotName(slot) + "”需要重新保存");
            return;
        }
        try {
            String targetOffsets = preferences.getString(valueKey(slot, "kst_ofs"), null);
            String fullCoordinates = preferences.getString(
                    "slot." + slot + ".gmpf_full", null);
            setStatus("●  正在应用“" + getSlotName(slot) + "”…", COLOR_BLUE);
            GmpfKeystoneBridge.ApplyResult result = gmpfBridge.apply(fullCoordinates);
            if (!result.targetWasValid) {
                throw new IllegalStateException(
                        "校正数据校验失败；目标=" + result.requestedCorners);
            }
            if (!result.matches) {
                throw new IllegalStateException(
                        "硬件回读不一致；目标=" + result.requestedCorners
                                + "，硬件=" + result.actualCorners);
            }
            for (String key : ENV_KEYS) {
                if (preferences.getBoolean(presentKey(slot, key), false)) {
                    String expected = preferences.getString(valueKey(slot, key), "");
                    bridge.setEnvironment(key, expected);
                    String actual = bridge.getEnvironment(key);
                    if (!expected.equals(actual)) {
                        throw new IllegalStateException(key + " 写入校验失败");
                    }
                }
            }
            String finalOffsets = bridge.getEnvironment("kst_ofs");
            if (targetOffsets == null || !targetOffsets.equals(finalOffsets)) {
                throw new IllegalStateException("kst_ofs 环境回读不一致");
            }
            String name = getSlotName(slot);
            setStatus("●  “" + name + "”已应用并通过硬件校验", COLOR_GREEN);
            showMessage("已应用“" + name + "”");
        } catch (RemoteException error) {
            setStatus("●  恢复失败：" + error.getMessage(), Color.rgb(255, 112, 112));
        } catch (RuntimeException error) {
            setStatus("●  应用失败：" + error.getMessage(), Color.rgb(255, 112, 112));
        }
    }

    private void refreshAllSlots() {
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            refreshSlot(slot);
        }
    }

    private void refreshSlot(int slot) {
        slotNameViews[slot - 1].setText(getSlotName(slot));
        TextView state = slotStateViews[slot - 1];
        TextView time = slotTimeViews[slot - 1];
        if (hasCompleteSlot(slot)) {
            state.setText("● 可用");
            state.setTextColor(COLOR_GREEN);
            long savedAt = preferences.getLong("slot." + slot + ".saved_at", 0L);
            String formatted = new SimpleDateFormat("MM-dd  HH:mm", Locale.getDefault())
                    .format(new Date(savedAt));
            time.setText("保存于 " + formatted);
            time.setTextColor(Color.rgb(184, 196, 213));
            actionButtons[(slot - 1) * 2].setText("覆盖保存");
        } else if (preferences.contains("slot." + slot + ".saved_at")) {
            state.setText("● 需更新");
            state.setTextColor(COLOR_AMBER);
            time.setText("旧版数据，请在目标画面下重新保存");
            time.setTextColor(COLOR_AMBER);
            actionButtons[(slot - 1) * 2].setText("重新保存");
        } else {
            state.setText("● 空槽位");
            state.setTextColor(COLOR_MUTED);
            time.setText("可保存当前校正数据");
            time.setTextColor(COLOR_MUTED);
            actionButtons[(slot - 1) * 2].setText("保存当前");
        }
    }

    private boolean hasCompleteSlot(int slot) {
        return preferences.contains("slot." + slot + ".saved_at")
                && preferences.contains("slot." + slot + ".gmpf_full");
    }

    private String getSlotName(int slot) {
        return preferences.getString(SettingsActivity.slotNameKey(slot), "预设 " + slot);
    }

    private String presentKey(int slot, String key) {
        return "slot." + slot + ".present." + key;
    }

    private String valueKey(int slot, String key) {
        return "slot." + slot + ".value." + key;
    }

    private void setActionsEnabled(boolean enabled) {
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            actionButtons[(slot - 1) * 2].setEnabled(enabled);
            actionButtons[(slot - 1) * 2 + 1]
                    .setEnabled(enabled && hasCompleteSlot(slot));
        }
        configureFocusNavigation();
    }

    private void setStatus(String message, int color) {
        if (statusView != null) {
            statusView.setText(message);
            statusView.setTextColor(color);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = margin;
        return params;
    }

    private GradientDrawable verticalGradient(int top, int bottom, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        drawable.setCornerRadius(radius);
        return drawable;
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
