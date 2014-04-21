package edu.utexas.quietplaces.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import edu.utexas.quietplaces.Config;
import edu.utexas.quietplaces.MainActivity;

/**
 * Base class for all of our fragments (except SettingsFragment and NavigationDrawerFragment)
 */
public class BaseFragment extends Fragment {

    public static final String FRAG_PACKAGE = Config.PACKAGE_NAME + ".fragments";

    public void shortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

/*
    public void longToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }
*/

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    protected static final String ARG_SECTION_NUMBER = "section_number";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Bundle args = getArguments();
        if (args != null) {
            ((MainActivity) activity).onSectionAttached(
                    args.getInt(ARG_SECTION_NUMBER));
        }
    }

}
