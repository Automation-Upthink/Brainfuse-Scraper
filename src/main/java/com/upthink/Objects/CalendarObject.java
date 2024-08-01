package com.upthink.Objects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CalendarObject {
    private Date date;
    private String timespan;
    private String timezone;

    public CalendarObject(Date date, String timespan, String timezone) {
        this.date = date;
        this.timespan = timespan;
        this.timezone = timezone;
    }

    public Date getDate() {
        return date;
    }

    public String getTimespan() {
        return timespan;
    }

    public String getTimezone() {
        return timezone;
    }


    @Override
    public String toString() {
        return "CalendarObject{" +
                "date=" + getDate() +
                ", timeSpan='" + getTimespan() + '\'' +
                ", timeZone='" + getTimezone() + '\'' +
                '}';
    }
}
