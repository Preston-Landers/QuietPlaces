package edu.utexas.quietplaces;


import org.joda.time.DateTime;

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
        if (latitude > 0.0) {
            latStr = String.format("%.3f N", latitude);
        }
        else {
            latStr = String.format("%.3f S", Math.abs(latitude));
        }
        if (longitude < 0.0) {
            longStr = String.format("%.3f W", Math.abs(longitude));
        } else {
            longStr = String.format("%.3f E", longitude);
        }
        return latStr + ", " + longStr;
    }
}
