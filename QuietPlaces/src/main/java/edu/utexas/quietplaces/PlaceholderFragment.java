package edu.utexas.quietplaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends QPFragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    protected static PlaceholderFragment newInstance(int sectionNumber) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        if (rootView != null) {
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            if (textView != null) {
                textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            }
        }
        return rootView;
    }

}
