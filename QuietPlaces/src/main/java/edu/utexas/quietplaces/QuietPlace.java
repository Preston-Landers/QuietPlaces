package edu.utexas.quietplaces;


import org.joda.time.DateTime;

/**
 * Data model object for a single QuietPlace as stored in the database.
 * The QuietPlaceMapMarker class is also important because it combines
 * this object plus the Google map marker and related functionality.
 */
public class QuietPlace {
    private long id;
    private String comment;
    private double latitude;
    private double longitude;
    private double radius;
    private DateTime datetime;
    private String category;
    private boolean autoadded;
    private String gplace_id;
    private String gplace_ref;

    @Override
    public String toString() {
/*
        return String.format(
                "QP Lat: %s Long: %s Rad: %s - Cat: %s - %s - %s",
                getLatitude(), getLongitude(), getRadius(), getCategory(),
                getDatetimeString(), getComment()
        );
*/
        return getHistoryEventFormatted();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
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


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRadiusFormatted() {
        double radius = getRadius();
        if (radius <= 1000) {
            return String.format("%.2f m", radius);
        } else {
            return String.format("%.2f km", (radius/1000));
        }
    }

    /**
     * Returns a formatted string with lat/long
     * @return string
     */
    public String getLocationFormatted() {
        double latitude = getLatitude();
        double longitude = getLongitude();
        String latStr, longStr;
        if (latitude > 0) {
            latStr = String.format("%.5f N", latitude);
        }
        else {
            latStr = String.format("%.5f S", Math.abs(latitude));
        }
        if (longitude < 0) {
            longStr = String.format("%.5f W", Math.abs(longitude));
        } else {
            longStr = String.format("%.5f E", longitude);
        }
        return latStr + ", " + longStr;
    }

    /**
     * Returns a suitable description of this quiet place
     * for the history event message when we add it to the DB.
     * @return a formatted string representation of the current status
     */
    public String getHistoryEventFormatted() {
        // is this good enough?
        return String.format(
                "%s: %s %s",
                getComment(), getLocationFormatted(), getRadiusFormatted()
        );
    }

    public String getGplace_id() {
        return gplace_id;
    }

    public void setGplace_id(String gplace_id) {
        this.gplace_id = gplace_id;
    }

    public String getGplace_ref() {
        return gplace_ref;
    }

    public void setGplace_ref(String gplace_ref) {
        this.gplace_ref = gplace_ref;
    }

    public boolean isAutoadded() {
        return autoadded;
    }

    public void setAutoadded(boolean autoadded) {
        this.autoadded = autoadded;
    }
}
