package edu.utexas.quietplaces;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A cursor adapter for the HistoryFragment ListView
 *
 * Basically this loads history item layout based on a record from the database.
 *
 */
public class HistoryCursorAdapter extends CursorAdapter {

    private static final String TAG = Config.PACKAGE_NAME + ".HistoryCursorAdapter";
    private LayoutInflater mInflater;

    public HistoryCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        HistoryEvent event = HistoryEventsContentProvider.cursorToHistoryEvent(cursor);

        if (cursor.getPosition() % 2 == 1) {
            view.setBackgroundColor(context.getResources().getColor(R.color.item_history_event_bg_odd));
        } else {
            view.setBackgroundColor(context.getResources().getColor(R.color.item_history_event_bg_even));
        }

        TextView tvEventText = (TextView) view.findViewById(R.id.event_text);
        tvEventText.setText(event.getText());

        TextView tvEventDate = (TextView) view.findViewById(R.id.event_date);
        tvEventDate.setText(DateUtils.getPrettyDateTime(event.getDatetime()));

        // Handle special event types and icons here
        String eventType = event.getType();
        TextView tvEventType = (TextView) view.findViewById(R.id.event_type);
        ImageView ivEventTypeIcon = (ImageView) view.findViewById(R.id.event_icon);

        // Love me some big if-blocks

        if (eventType.equals(HistoryEvent.TYPE_DATABASE_LOADED)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_database_loaded));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_rotate);

        } else if (eventType.equals(HistoryEvent.TYPE_HISTORY_CLEARED)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_history_cleared));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_clear_playlist);

        } else if (eventType.equals(HistoryEvent.TYPE_PLACE_ADD)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_place_add));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_btn_add);

        } else if (eventType.equals(HistoryEvent.TYPE_PLACE_REMOVE)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_place_remove));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_delete);

        } else if (eventType.equals(HistoryEvent.TYPE_PLACE_UPDATE)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_place_update));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_save);

        } else if (eventType.equals(HistoryEvent.TYPE_PLACE_ENTER)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_place_enter));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_forward);

        } else if (eventType.equals(HistoryEvent.TYPE_PLACE_EXIT)) {
            tvEventType.setText(context.getResources().getString(R.string.event_type_place_exit));
            ivEventTypeIcon.setImageResource(R.drawable.ic_menu_revert);

        } else {
            Log.w(TAG, "Warning: unknown event type: " + eventType);
            tvEventType.setText(eventType);
            // Leave the default icon.
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.item_history_event, parent, false);
    }
}
