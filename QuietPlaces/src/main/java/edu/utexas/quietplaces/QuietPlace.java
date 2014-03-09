package edu.utexas.quietplaces;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Data model object for a single QuietPlace
 */
public class QuietPlace {
    private long id;
    private String comment;
    private double latitude;
    private double longitude;
    private double radius;
    private DateTime datetime;
    private String category;

    @Override
    public String toString() {
        return String.format(
                "QP Lat: %s Long: %s Rad: %s - Cat: %s - %s - %s",
                getLatitude(), getLongitude(), getRadius(), getCategory(),
                getDatetimeString(), getComment()
        );
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
}
