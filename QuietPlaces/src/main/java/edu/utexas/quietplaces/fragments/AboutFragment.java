package edu.utexas.quietplaces.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.utexas.quietplaces.R;

/**
 * A fragment for the About / Help screen.
 */
public class AboutFragment extends BaseFragment {
    private static final String TAG = FRAG_PACKAGE + ".AboutFragment";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AboutFragment newInstance(int sectionNumber) {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public AboutFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        if (rootView == null) {
            Log.e(TAG, "No root view in onCreateView in AboutFragment!");
            return null;
        }

        // Plug in the version
        TextView aboutVersion = (TextView) rootView.findViewById(R.id.about_version_tv);
        if (aboutVersion != null) {
            try {
                //noinspection ConstantConditions
                PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                aboutVersion.setText(getString(R.string.about_version) + pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to read app version info (name not found)", e);
            } catch (NullPointerException e) {
                Log.e(TAG, "Unable to read app version info (null pointer)", e);
            }
        }

        // Enable links
        TextView aboutCodeLink = (TextView) rootView.findViewById(R.id.about_code_link);
        if (aboutCodeLink != null) {
            aboutCodeLink.setText(Html.fromHtml(getString(R.string.about_code_link)));
            aboutCodeLink.setMovementMethod(LinkMovementMethod.getInstance());
        }
        TextView toolsTV = (TextView) rootView.findViewById(R.id.help_tools_tv);
        if (toolsTV != null) {
            toolsTV.setText(Html.fromHtml(getString(R.string.help_tools)));
            toolsTV.setMovementMethod(LinkMovementMethod.getInstance());
        }
        TextView conclTV = (TextView) rootView.findViewById(R.id.help_conclusion_tv);
        if (conclTV != null) {
            conclTV.setText(Html.fromHtml(getString(R.string.help_conclusion)));
            conclTV.setMovementMethod(LinkMovementMethod.getInstance());
        }

        return rootView;
    }

}
