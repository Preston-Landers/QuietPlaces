package edu.utexas.quietplaces;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment containing an event history view.
 */
public class HistoryFragment
        extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = Config.PACKAGE_NAME + ".HistoryFragment";

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    protected static final String ARG_SECTION_NUMBER = "section_number";

    private SimpleCursorAdapter adapter;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    protected static HistoryFragment newInstance(int sectionNumber) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public HistoryFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Bundle args = getArguments();
        if (args != null) {
            ((MainActivity) activity).onSectionAttached(
                    args.getInt(ARG_SECTION_NUMBER));
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // registerForContextMenu(getListView());
        fillData();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);
        return rootView;
    }

    public void clickClearHistoryButton(View view) {
        Log.w(TAG, "Someone clicked the Clear History button.");
        MainActivity activity = (MainActivity) getActivity();

        HistoryEventsContentProvider.deleteAllHistoryEvents(activity);

        HistoryEvent.logEvent(activity,
                HistoryEvent.TYPE_HISTORY_CLEARED,
                "History cleared."
        );
    }

    // creates a new loader after the initLoader () call
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = HistoryEventsTable.allColumns ;
        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                HistoryEventsContentProvider.CONTENT_URI, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // data is not available anymore, delete reference
        adapter.swapCursor(null);
    }

    private void fillData() {

        // Fields from the database (projection)
        // Must include the _id column for the adapter to work
        // String[] from = HistoryEventsTable.allColumns;
        String[] from = {
                // HistoryEventsTable.COLUMN_TYPE,
                HistoryEventsTable.COLUMN_TEXT,
                HistoryEventsTable.COLUMN_DATETIME
        };

        // Fields on the UI to which we map
        int[] to = new int[] {
                // R.id.event_type,
                R.id.event_text,
                R.id.event_date
        };

        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(getActivity(),
                R.layout.item_history_event, null,
                from, to, 0);

        setListAdapter(adapter);
    }

}
