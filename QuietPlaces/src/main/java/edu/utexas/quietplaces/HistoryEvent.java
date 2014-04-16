package edu.utexas.quietplaces;

import android.content.Context;
import org.joda.time.DateTime;

/**
 * The history table contains a database of system events.
 * This class represents a single event and contains a static logEvent() method
 * that is the main interface to create events.
 */
public class HistoryEvent {

    public static final String TYPE_PLACE_ADD = "event_type_place_add";
    public static final String TYPE_PLACE_REMOVE = "event_type_place_remove";
    public static final String TYPE_PLACE_UPDATE = "event_type_place_update";
    public static final String TYPE_PLACE_ENTER = "event_type_place_enter";
    public static final String TYPE_PLACE_EXIT = "event_type_place_exit";
    public static final String TYPE_DATABASE_LOADED = "event_type_database_loaded";
    public static final String TYPE_HISTORY_CLEARED = "event_type_history_cleared";

    private long id;
    private String type;
    private String text;
    private DateTime datetime;
    private boolean seen;

    /**
     * This is the main interface for creating new history events in the system.
     * @param context activity context
     * @param eventType one of the constants defined by this class
     * @param eventText the string description of the event.
     * @return a HistoryEvent object which has been saved to the database.
     */
    public static HistoryEvent logEvent(
            Context context,
            String eventType,
            String eventText
    ) {
        HistoryEvent event = new HistoryEvent();
        event.setDatetime(new DateTime());
        event.setType(eventType);
        event.setText(eventText);

        return HistoryEventsContentProvider.saveHistoryEvent(context, event);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public DateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(DateTime datetime) {
        this.datetime = datetime;
    }

    public String getDatetimeString() {
        return DateUtils.getISO8601Date(getDatetime());
    }

    public void setDatetimeString(String datetimeString) {
        DateTime dt = DateUtils.parseISO8601Date(datetimeString);
        setDatetime(dt);
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getTypeString(Context context) {
        if (context == null) {
            return getText();
        }
        return context.getString(
                context.getResources().getIdentifier(
                        getType(), "string", context.getPackageName()
                )
        );
    }

    @Override
    public String toString() {
        return String.format(
                "%s %s: (%s) %s",
                getId(),
                DateUtils.getPrettyDateTime(getDatetime()),
                getTypeString(null),
                getText()
        );
    }

}
