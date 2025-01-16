package org.lineageos.settings.thermal;

import android.os.Bundle;

import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;

public class PerformanceModeSettingsFragment extends PreferenceFragment {
    private ThermalUtils mThermalUtils;
    private SwitchPreference mPerformanceModePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.thermal_settings);
        
        mThermalUtils = new ThermalUtils(getActivity());
        mPerformanceModePreference = (SwitchPreference) findPreference("always_performance_mode");
        mPerformanceModePreference.setChecked(mThermalUtils.isPerformanceModeEnabled());
        mPerformanceModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            mThermalUtils.setPerformanceModeEnabled((Boolean) newValue);
            return true;
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }
}