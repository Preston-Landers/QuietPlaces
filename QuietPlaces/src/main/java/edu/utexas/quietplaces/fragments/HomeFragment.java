package edu.utexas.quietplaces.fragments;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import edu.utexas.quietplaces.R;

/**
 * A fragment containing the welcome screen of the app.
 */
public class HomeFragment extends BaseFragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HomeFragment newInstance(int sectionNumber) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateSwitchWithRingerState();
    }

    /**
     * Update the ringer switch on the home screen to the current ringer state on app startup.
     */
    private void updateSwitchWithRingerState() {
        Activity activity = getActivity();
        CheckBox ringerCheckBox = (CheckBox) activity.findViewById(R.id.checkbox_home_ringer);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            ringerCheckBox.setChecked(true);
        } else {
            ringerCheckBox.setChecked(false);
        }

    }

}
