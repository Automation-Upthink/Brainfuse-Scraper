package com.upthink.Objects;

public class RequestObject {

    private final String accountNumber, password, subject, accountType, status;
    private final boolean audioCertified;

    public RequestObject(String accountNumber, String password, String subject, String accountType, String status, boolean audioCertified) {
        this.accountNumber = accountNumber;
        this.password = password;
        this.subject = subject;
        this.accountType = accountType;
        this.status = status;
        this.audioCertified = audioCertified;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getSubject() {
        return subject;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getStatus() {
        return status;
    }

    public boolean isAudioCertified() {
        return audioCertified;
    }
}
