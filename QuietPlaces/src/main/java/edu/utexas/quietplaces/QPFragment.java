package edu.utexas.quietplaces;

import android.support.v4.app.Fragment;
import android.widget.Toast;

/**
 * Base class for all of our fragments.
 */
public class QPFragment extends Fragment {
    public void shortToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    public void longToast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
    }

}
