package edu.utexas.quietplaces;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;


/**
 * Standard settings fragment.
 */
public class SettingsFragment extends PreferenceFragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final int SECTION_NUMBER = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                SECTION_NUMBER);
    }

}
