package edu.utexas.quietplaces;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    public static final String KEY_USE_LOCATION = "pref_use_location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
