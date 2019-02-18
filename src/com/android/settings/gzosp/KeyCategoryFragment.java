package com.android.settings.gzosp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceDataStore;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.gzosp.activities.ShortcutPickerActivity;

import java.util.LinkedHashMap;

import asylum.provider.AsylumSettings;
import com.asylum.action.ActionsArray;
import com.asylum.action.ActionConstants;
import asylum.preference.SettingPreference;
import asylum.preference.SettingSwitchPreference;
import asylum.preference.PreferenceManager;
import com.asylum.keys.HwKeyHelper;
import com.asylum.utils.ShortcutPickerHelper;

import com.asylum.keys.parser.Key;
import com.asylum.keys.parser.KeyCategory;
import com.asylum.keys.parser.KeyParser;

public class KeyCategoryFragment extends SettingsPreferenceFragment
        implements ShortcutPickerHelper.OnPickListener {

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    KeyCategory mCategory;

    public KeyCategoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);
        mCategory = KeyParser.parseKeys(getActivity()).get(getArguments().getString("key"));

        setPreferenceScreen(reloadSettings());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    protected int getPreferenceScreenResId() {
        return 0;
    }

    @Override
    public void shortcutPicked(String action, String description,
            Bitmap bmp, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        PreferenceManager.putStringInSettings(getContext(), 0, mPendingSettingsKey, action);
        reloadSettings();
        mPendingSettingsKey = null;
    }

    @Override
    public int getMetricsCategory() {
        return 1;
    }

    private PreferenceScreen reloadSettings() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs == null) {
            prefs = getPreferenceManager().createPreferenceScreen(getContext());
        } else {
            prefs.removeAll();
        }

        TypedValue themeTV = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.preferenceTheme, themeTV, true);
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), themeTV.resourceId);

        if (mCategory.allowDisable) {
            SettingSwitchPreference disable = new SettingSwitchPreference(contextWrapper);
            disable.setTitle(getActivity().getString(R.string.disable_key_category_title, mCategory.name));
            disable.setKey(mCategory.key + "_disabled");
            prefs.addPreference(disable);
        }
        for (Key key : mCategory.keys) {
            android.util.Log.d("TEST", "key - " + key.name + " : supports multiple actions - " + key.supportsMultipleActions);
            if (key.supportsMultipleActions) {
                createPreferenceCategory(prefs, key.name, key.keyCode, key.order);
            } else {
                KeyPreference pref = new KeyPreference(getContext(), key);
                if (key.order != 0) {
                    prefs.setOrder(key.order);
                }
                prefs.addPreference(pref);
            }
        }
        return prefs;
    }

    private String getStringFromSettings(String key, String def) {
        String val = AsylumSettings.System.getStringForUser(
                getActivity().getContentResolver(), key, UserHandle.USER_CURRENT);
        return (val == null) ? def : val;
    }

    private void createPreferenceCategory(PreferenceScreen prefs, String title, int keyCode, int order) {
        PreferenceCategory category = new PreferenceCategory(getActivity());
        category.setTitle(title);
        if (order != 0) {
            category.setOrder(order);
        }
        prefs.addPreference(category);

        String key = KeyEvent.keyCodeToString(keyCode);
        key = key.replace("KEYCODE_", "key_").toLowerCase();

        // Normal Press
        KeyPreference press = new KeyPreference(getActivity());
        press.setTitle(getActivity().getString(R.string.keys_action_normal));
        press.setDialogTitle(press.getTitle());
        press.setKey(key + "_action");
        String action = getStringFromSettings(press.getKey(),
                HwKeyHelper.getDefaultTapActionForKeyCode(getActivity(), keyCode));
        press.setDefaultValue(HwKeyHelper.getDefaultTapActionForKeyCode(getActivity(), keyCode));
        press.setValue(action);
        category.addPreference(press);

        // Long Press
        KeyPreference longpress = new KeyPreference(getActivity());
        longpress.setTitle(getActivity().getString(R.string.keys_action_long));
        longpress.setDialogTitle(longpress.getTitle());
        longpress.setKey(key + "_long_press_action");
        action = getStringFromSettings(longpress.getKey(),
                HwKeyHelper.getDefaultLongPressActionForKeyCode(getActivity(), keyCode));
        longpress.setDefaultValue(HwKeyHelper.getDefaultLongPressActionForKeyCode(getActivity(), keyCode));
        longpress.setValue(action);
        category.addPreference(longpress);

        // Double Tap
        KeyPreference doubletap = new KeyPreference(getActivity());
        doubletap.setTitle(getActivity().getString(R.string.keys_action_double));
        doubletap.setDialogTitle(doubletap.getTitle());
        doubletap.setKey(key + "_double_tap_action");
        action = getStringFromSettings(doubletap.getKey(),
                HwKeyHelper.getDefaultDoubleTapActionForKeyCode(getActivity(), keyCode));
        doubletap.setDefaultValue(HwKeyHelper.getDefaultDoubleTapActionForKeyCode(getActivity(), keyCode));
        doubletap.setValue(action);
        category.addPreference(doubletap);
    }

    private static class KeyPreference extends ListPreference {
        private final Context mContext;
        private int mKeyCode;

        public KeyPreference(Context context) {
            super(context);
            mContext = context;

            ActionsArray actionsArray = new ActionsArray(context, true);
            setEntries(actionsArray.getEntries());
            setEntryValues(actionsArray.getValues());
            setSummary("%s");
            setPreferenceDataStore(new DataStore());
        }

        public KeyPreference(Context context, Key key) {
            this(context);
            mKeyCode = key.keyCode;
            setTitle(key.name);
            setKey(KeyParser.getPreferenceKey(mKeyCode));
            setDefaultValue(key.def);
            if (key.drawableId > 0) {
                //setIcon(key.drawableId);
            }
            setDialogTitle(key.name);
        }

        @Override
        public boolean callChangeListener(final Object newValue) {
            final String action = String.valueOf(newValue);
            if (action.equals(ActionConstants.ACTION_APP)) {
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getStringExtra(ShortcutPickerHelper.EXTRA_ACTION);
                        callChangeListener(action);
                        mContext.unregisterReceiver(this);
                    }
                }, new IntentFilter(ShortcutPickerHelper.ACTION_SHORTCUT_PICKED));
                mContext.startActivity(new Intent(mContext, ShortcutPickerActivity.class));
                return false;
            }
            return super.callChangeListener(newValue);
        }

        protected String getString(String key, String defValue) {
            return PreferenceManager.getStringFromSettings(getContext(), 0, getKey(),
                defValue);
        }

        protected boolean putString(String key, String value) {
            if (TextUtils.equals(value, getString(key, null))) {
                return true;
            }
            PreferenceManager.putStringInSettings(getContext(),
                    0, getKey(), value);
            return true;
        }

        protected boolean isPersisted() {
            return PreferenceManager.settingExists(getContext(), 0, getKey());
        }

        private class DataStore extends PreferenceDataStore {
            @Override
            public void putString(String key, String value) {
                KeyPreference.this.putString(key, value);
            }

            @Override
            public String getString(String key, String defaultValue) {
                return KeyPreference.this.getString(key, defaultValue);
            }
        }
    }
}
