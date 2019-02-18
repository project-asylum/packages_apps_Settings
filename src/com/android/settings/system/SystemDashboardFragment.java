/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.system;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.backup.BackupSettingsActivityPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.Arrays;
import java.util.List;

import asylum.preference.SettingPreference;
import com.asylum.keys.parser.KeyCategory;
import com.asylum.keys.parser.KeyParser;

public class SystemDashboardFragment extends DashboardFragment {

    private static final String TAG = "SystemDashboardFrag";

    private static final String KEY_RESET = "reset_dashboard";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final PreferenceScreen screen = getPreferenceScreen();
        // We do not want to display an advanced button if only one setting is hidden
        if (getVisiblePreferenceCount(screen) == screen.getInitialExpandedChildrenCount() + 1) {
            screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_SYSTEM_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_dashboard_fragment;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_system_dashboard;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        TypedValue themeTV = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.preferenceTheme, themeTV, true);
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), themeTV.resourceId);

        for (KeyCategory category : KeyParser.parseKeys(getActivity()).values()) {
            SettingPreference pref = new SettingPreference(contextWrapper);
            pref.setFragment("com.android.settings.gzosp.KeyCategoryFragment");
            pref.getExtras().putString("key", category.key);
            pref.setTitle(category.name);
            android.util.Log.d("TEST", "drawableId - " + category.drawableId);
            if (category.drawableId > 0) {
                pref.setIcon(Icon.createWithResource(contextWrapper, category.drawableId).loadDrawable(contextWrapper));
                //pref.setIcon(contextWrapper.getResources().getDrawable(category.drawableId));
            }
            pref.setKey(category.key);
            pref.setOrder(-255);
            getPreferenceScreen().addPreference(pref);
        }
    }
    private int getVisiblePreferenceCount(PreferenceGroup group) {
        int visibleCount = 0;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                visibleCount += getVisiblePreferenceCount((PreferenceGroup) preference);
            } else if (preference.isVisible()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.system_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add((new BackupSettingsActivityPreferenceController(
                            context).getPreferenceKey()));
                    keys.add(KEY_RESET);
                    return keys;
                }
            };
}
