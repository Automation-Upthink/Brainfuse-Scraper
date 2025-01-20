package com.upthink.Objects;

import com.upthink.Objects.CalendarObject;

import java.util.Date;

public class ResponseObject {

    private Date date;
    private final CalendarObject calendarObject;
    private final String day, accountNumber, subject, accountType, status;


    public ResponseObject(CalendarObject calendarObject, String day, String accountNumber, String subject, String accountType, String status) {
        this.calendarObject = calendarObject;
        this.day = day;
        this.accountNumber = accountNumber;
        this.subject = subject;
        this.accountType = accountType;
        this.status = status;
    }
}
