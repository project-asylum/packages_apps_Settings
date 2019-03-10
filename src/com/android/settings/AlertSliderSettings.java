/*
 * Copyright (C) 2014 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlertSliderSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {
    private static final String TAG = "SystemSettings";

    private static final String KEY_SWAP_SLIDER_ORDER = "swap_slider_order";

    private static final String EMPTY_STRING = "";

    private Handler mHandler;

    private boolean mHasAlertSlider = false;

    private SwitchPreference mSwapSliderOrder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.alert_slider_settings);

        mHandler = new Handler();

        final Resources res = getActivity().getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mHasAlertSlider = res.getBoolean(com.android.internal.R.bool.config_hasAlertSlider)
                && !TextUtils.isEmpty(res.getString(
                        com.android.internal.R.string.alert_slider_state_path))
                && !TextUtils.isEmpty(res.getString(
                        com.android.internal.R.string.alert_slider_uevent_match_path));

        /* Swap Slider order */
        mSwapSliderOrder = (SwitchPreference) findPreference(KEY_SWAP_SLIDER_ORDER);
        if (mSwapSliderOrder != null) {
            if (mHasAlertSlider) {
                mSwapSliderOrder.setOnPreferenceChangeListener(this);
            } else {
                mSwapSliderOrder = null;
                removePreference(KEY_SWAP_SLIDER_ORDER);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE;
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        if (list != null) {
            list.setValue(Integer.toString(value));
            list.setSummary(list.getEntry());
            list.setOnPreferenceChangeListener(this);
        }
        return list;
    }

    private boolean handleOnPreferenceTreeClick(Preference preference) {
        return false;
    }

    private boolean handleOnPreferenceChange(Preference preference, Object newValue) {
        final String setting = getSystemPreferenceString(preference);

        if (TextUtils.isEmpty(setting)) {
            // No system setting.
            return false;
        }

        if (preference != null && preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            String value = (String) newValue;
            int index = listPref.findIndexOfValue(value);
            listPref.setSummary(listPref.getEntries()[index]);
            Settings.System.putIntForUser(getContentResolver(), setting, Integer.valueOf(value),
                    UserHandle.USER_CURRENT);
        } else if (preference != null && preference instanceof SwitchPreference) {
            boolean state = false;
            if (newValue instanceof Boolean) {
                state = (Boolean) newValue;
            } else if (newValue instanceof String) {
                state = Integer.valueOf((String) newValue) != 0;
            }
            Settings.System.putIntForUser(getContentResolver(), setting, state ? 1 : 0,
                    UserHandle.USER_CURRENT);
        }

        return true;
    }

    private String getSystemPreferenceString(Preference preference) {
        if (preference == null) {
            return EMPTY_STRING;
        } else if (preference == mSwapSliderOrder) {
            return Settings.System.ALERT_SLIDER_ORDER;
        }

        return EMPTY_STRING;
    }

    private void reload() {
        final ContentResolver resolver = getActivity().getContentResolver();
        final Resources res = getActivity().getResources();

        final boolean swapSliderOrderEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.ALERT_SLIDER_ORDER, 0, UserHandle.USER_CURRENT) != 0;

        if (mSwapSliderOrder != null) {
            mSwapSliderOrder.setChecked(swapSliderOrderEnabled);
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean handled = handleOnPreferenceChange(preference, newValue);
        if (handled) {
            reload();
        }
        return handled;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final boolean handled = handleOnPreferenceTreeClick(preference);
        // return super.onPreferenceTreeClick(preferenceScreen, preference);
        return handled;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.alert_slider_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                final int myUserId = UserHandle.myUserId();
                final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;

                // TODO: Implement search index provider.

                return result;
            }
        };
}
