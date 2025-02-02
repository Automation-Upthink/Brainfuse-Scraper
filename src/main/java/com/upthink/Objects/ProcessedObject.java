package com.upthink.Objects;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProcessedObject {
    private String username;
    private ArrayList<CalendarObject> calendarObjects;
    private String subject;
    private String singleDual;
    private String audioCertified;

    public ProcessedObject(String username, ArrayList<CalendarObject> calendarObjects, String subject, String singleDual, String audioCertified) {
        this.username = username;
        this.calendarObjects = calendarObjects;
        this.subject = subject;
        this.singleDual = singleDual;
        this.audioCertified = audioCertified;
    }

    // Getters and setters for each field
    public String getUsername() {
        return username;
    }


    public ArrayList<CalendarObject> getCalendarObjects() {
        return calendarObjects;
    }


    public String getSubject() {
        return subject;
    }



    public String getSingleDual() {
        return singleDual;
    }


    public String getAudioCertified() {
        return audioCertified;
    }

    public List<List<String>> processObject() throws ParseException {
        // Format the Calendar Object
        List<List<String>> result = new ArrayList<>();
        for (CalendarObject calendarObject : calendarObjects) {
            result.add(calendarObjectFormatter(calendarObject));

        }

        return result;
    }

    private ArrayList calendarObjectFormatter(CalendarObject calendarObject) throws ParseException {
        Date dateObject = calendarObject.getDate();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
        String day = dayFormat.format(dateObject);
        String date = dateFormat.format(dateObject);
        String timespan = calendarObject.getTimespan();
        String startTime;
        String endTime;
        String shiftAvailable;
        String shiftDuration;
        if(timespan.equals("-")) {
            startTime = "-";
            endTime = "-";
            shiftAvailable = "No";
            shiftDuration = "-";
        } else {
            String[] timeSpanArray = timespan.split("to");
            startTime = ProcessedObject.formatTime(timeSpanArray[0]);
            endTime = ProcessedObject.formatTime(timeSpanArray[1]);
            shiftDuration = ProcessedObject.shiftDuration(timeSpanArray[0], timeSpanArray[1]).toString();
            shiftAvailable = "Yes";
        }

        ArrayList result = new ArrayList<>(Arrays.asList(date, day, username, subject, singleDual,
                audioCertified, calendarObject.getTimezone(),
                startTime, endTime, shiftAvailable, shiftDuration
        ));
        return result;
    }

    private static Double shiftDuration(String startTime, String endTime) throws ParseException {
        // Define the time format
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
        // Parse the start and end times
        Date startDate = timeFormat.parse(startTime);
        Date endDate = timeFormat.parse(endTime);
        // Create calendar instances
        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        startCalendar.setTime(startDate);
        endCalendar.setTime(endDate);
        // Adjust the end time if it is on the next day
        if (startCalendar.get(Calendar.AM_PM) == Calendar.PM && endCalendar.get(Calendar.AM_PM) == Calendar.AM) {
            endCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        // Calculate the duration in milliseconds
        long durationInMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
        // Convert the duration to hours
        double durationInHours = (double) durationInMillis / (1000 * 60 * 60);
        return Math.round(durationInHours * 10.0) / 10.0;
    }

    private static String formatTime(String time) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat("h:mma");
        // Define the output format
        SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm:ss a");
        Date date = inputFormat.parse(time);
        return outputFormat.format(date);
    }

}
