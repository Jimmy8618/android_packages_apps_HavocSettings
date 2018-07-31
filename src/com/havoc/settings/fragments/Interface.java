/*
 * Copyright (C) 2018 Havoc-OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.havoc.settings.fragments;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.FontInfo;
import android.content.IFontService;
import android.content.Intent; 
import android.content.IntentFilter; 
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen; 
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;
import android.support.v14.preference.SwitchPreference; 

import android.content.pm.PackageInfo; 
import android.content.pm.PackageManager; 
import android.content.pm.PackageManager.NameNotFoundException; 
import android.content.res.Resources; 
import android.util.Log; 
import android.content.Context; 
import com.havoc.settings.preferences.Utils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto;

import com.havoc.settings.fragments.ui.FontDialogPreference;
import com.havoc.settings.preferences.SystemSettingSeekBarPreference; 
import com.havoc.settings.R;

import java.util.ArrayList;
import java.util.List;

public class Interface extends SettingsPreferenceFragment implements 
    Preference.OnPreferenceChangeListener {

    private static final String KEY_FONT_PICKER_FRAGMENT_PREF = "custom_font";
    private static final String SUBS_PACKAGE = "projekt.substratum";
    private static final String CATEGORY_SUBSTRATUM = "category_substratum"; 
    private static final String SYSUI_ROUNDED_SIZE = "sysui_rounded_size"; 
    private static final String QS_TILE_CIRCLE = "qs_tile_circle"; 

    private SwitchPreference mQsTileCircle; 

    private Context mContext; 
    private SystemSettingSeekBarPreference mCornerRadius; 
	
    private FontDialogPreference mFontPreference;
    private IFontService mFontService;

    private IntentFilter mIntentFilter; 
 
    private PreferenceCategory substratumCategory; 
	
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { 
        @Override 
        public void onReceive(Context context, Intent intent) { 
            String action = intent.getAction(); 
            if (action.equals("com.android.server.ACTION_FONT_CHANGED")) { 
                mFontPreference.stopProgress(); 
            } 
        } 
    }; 
 
    @Override 
    public void onResume() { 
        super.onResume(); 
        final Context context = getActivity(); 
        context.registerReceiver(mIntentReceiver, mIntentFilter); 
    } 
 
    @Override 
    public void onPause() { 
        super.onPause(); 
        final Context context = getActivity(); 
        context.unregisterReceiver(mIntentReceiver); 
        mFontPreference.stopProgress(); 
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.havoc_settings_interface);
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen(); 		

        substratumCategory = 
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_SUBSTRATUM); 
				
        mFontPreference =  (FontDialogPreference) findPreference(KEY_FONT_PICKER_FRAGMENT_PREF);
        mFontService = IFontService.Stub.asInterface(
                ServiceManager.getService("havocfont"));

        if (!isPackageInstalled(SUBS_PACKAGE, getActivity())) {
            mFontPreference.setSummary(getCurrentFontInfo().fontName.replace("_", " "));
            prefScreen.removePreference(substratumCategory);		
        } else {
            mFontPreference.setSummary(getActivity().getString(
                    R.string.disable_fonts_installed_title));
        }

        mQsTileCircle = 
            (SwitchPreference) findPreference(QS_TILE_CIRCLE); 
            mQsTileCircle.setChecked(Settings.System.getInt(getContentResolver(), 
                Settings.System.QS_TILE_CIRCLE, 1) == 1); 
                mQsTileCircle.setOnPreferenceChangeListener(this); 

        mIntentFilter = new IntentFilter(); 
        mIntentFilter.addAction("com.android.server.ACTION_FONT_CHANGED"); 		

        // Rounded Corner Radius 
        mCornerRadius = (SystemSettingSeekBarPreference) findPreference(SYSUI_ROUNDED_SIZE); 
        int cornerRadius = Settings.Secure.getInt(getContentResolver(), 
                Settings.Secure.SYSUI_ROUNDED_SIZE, 0); 
        mCornerRadius.setValue(cornerRadius / 1); 
        mCornerRadius.setOnPreferenceChangeListener(this); 
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) { 
        final Context context = getActivity(); 
        if (preference == mCornerRadius) { 
            int value = (Integer) newValue; 
            Settings.Secure.putInt(getContentResolver(), 
                Settings.Secure.SYSUI_ROUNDED_SIZE, value * 1); 
        } else if (preference == mQsTileCircle) { 
            Settings.System.putInt(getContentResolver(), 
                    Settings.System.QS_TILE_CIRCLE,  (Boolean) newValue ? 1 : 0); 
                    Utils.showSystemUiRestartDialog(context);
            return true; 
        }
        return true; 
    } 

    private FontInfo getCurrentFontInfo() {
        try {
            return mFontService.getFontInfo();
        } catch (RemoteException e) {
            return FontInfo.getDefaultFontInfo();
        }
    }

    private boolean isPackageInstalled(String package_name, Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }	

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.HAVOC_SETTINGS;
    }
}
