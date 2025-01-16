package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.os.UserHandle;
import androidx.preference.PreferenceManager;

public final class ThermalUtils {
    private static final String THERMAL_CONTROL = "thermal_control";
    private static final String PERFORMANCE_MODE_ENABLED = "performance_mode_enabled";
    
    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_GAMING = 1;
    protected static final int STATE_PERF = 2;
    
    private static final String VENDOR_THERMAL_PROP = "vendor.thermal.mode";
    private static final String MODE_EQS = "eqs";
    private static final String MODE_GAME_PERF = "game-perf";
    private static final String MODE_PERF = "perf";
    
    private SharedPreferences mSharedPrefs;
    private String mCurrentThermalMode = null;  // Cache for current thermal mode
    
    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Initialize current thermal mode from system property
        mCurrentThermalMode = SystemProperties.get(VENDOR_THERMAL_PROP, MODE_EQS);
    }
    
    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, ThermalService.class),
                UserHandle.CURRENT);
    }

    protected boolean isPerformanceModeEnabled() {
        return mSharedPrefs.getBoolean(PERFORMANCE_MODE_ENABLED, false);
    }

    protected void setPerformanceModeEnabled(boolean enabled) {
        mSharedPrefs.edit().putBoolean(PERFORMANCE_MODE_ENABLED, enabled).apply();
        // Immediately apply the new thermal mode
        if (enabled) {
            SystemProperties.set(VENDOR_THERMAL_PROP, MODE_GAME_PERF);
            mCurrentThermalMode = MODE_GAME_PERF;
        } else {
            setDefaultThermalProfile();
        }
    }
    
    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }
    
    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);
        if (value == null || value.isEmpty()) {
            value = "gaming=:benchmark=";
            writeValue(value);
        }
        return value;
    }
    
    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        String[] modes = value.split(":");
        
        // Remove package from all modes first
        modes[0] = modes[0].replace(packageName + ",", "");
        modes[1] = modes[1].replace(packageName + ",", "");
        
        // Add package to appropriate mode
        switch (mode) {
            case STATE_GAMING:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_PERF:
                modes[1] = modes[1] + packageName + ",";
                break;
        }
        
        writeValue(modes[0] + ":" + modes[1]);
    }
    
    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        
        if (modes[0].contains(packageName + ",")) {
            return STATE_GAMING;
        } else if (modes[1].contains(packageName + ",")) {
            return STATE_PERF;
        }
        return STATE_DEFAULT;
    }
    
    protected void setThermalProfile(String packageName) {
        // If performance mode is enabled, always use game-perf
        if (isPerformanceModeEnabled()) {
            if (!MODE_GAME_PERF.equals(mCurrentThermalMode)) {
                SystemProperties.set(VENDOR_THERMAL_PROP, MODE_GAME_PERF);
                mCurrentThermalMode = MODE_GAME_PERF;
            }
            return;
        }

        // Otherwise, use the normal app-based profile switching
        String value = getValue();
        String[] modes = value.split(":");
        String newThermalMode = MODE_EQS;
        
        if (modes[0].contains(packageName + ",")) {
            newThermalMode = MODE_GAME_PERF; 
        } else if (modes[1].contains(packageName + ",")) {
            newThermalMode = MODE_PERF; 
        }
        
        // Only write the property if the thermal mode has changed
        if (!newThermalMode.equals(mCurrentThermalMode)) {
            SystemProperties.set(VENDOR_THERMAL_PROP, newThermalMode);
            mCurrentThermalMode = newThermalMode;
        }
    }
    
    protected void setDefaultThermalProfile() {
        // If performance mode is enabled, keep using game-perf
        if (isPerformanceModeEnabled()) {
            if (!MODE_GAME_PERF.equals(mCurrentThermalMode)) {
                SystemProperties.set(VENDOR_THERMAL_PROP, MODE_GAME_PERF);
                mCurrentThermalMode = MODE_GAME_PERF;
            }
            return;
        }

        // Only write the property if we're not already in default mode
        if (!MODE_EQS.equals(mCurrentThermalMode)) {
            SystemProperties.set(VENDOR_THERMAL_PROP, MODE_EQS);
            mCurrentThermalMode = MODE_EQS;
        }
    }
}