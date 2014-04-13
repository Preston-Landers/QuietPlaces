package edu.utexas.quietplaces;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

/**
 * A fragment containing the welcome screen of the app.
 */
public class HomeFragment extends QPFragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    protected static HomeFragment newInstance(int sectionNumber) {
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
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateSwitchWithRingerState();
    }

    private void updateSwitchWithRingerState() {
        Activity activity = getActivity();
        Switch ringerSwitch = (Switch) activity.findViewById(R.id.switch_home_ringer);
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            ringerSwitch.setChecked(true);
        } else {
            ringerSwitch.setChecked(false);
        }

    }

}
