package com.upthink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

public class CompositeKey {
    private final String startDate;
    private final String accountNumber;
    private final String subject;

    // Constructor
    public CompositeKey(String startDate, String accountNumber, String subject) {
        this.startDate = startDate;
        this.accountNumber = accountNumber;
        this.subject = subject;
    }

    // Getters
    public String getStartDate() {
        return startDate;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getSubject(){
        return subject;
    }

    // equals method for comparing two CompositeKey objects
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompositeKey that = (CompositeKey) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(accountNumber, that.accountNumber) &&
                Objects.equals(subject, that.subject);
    }

    // hashCode method for using CompositeKey in hash-based collections
    @Override
    public int hashCode() {
        return Objects.hash(startDate, accountNumber, subject);
    }

    // toString method for easy debugging (not used for serialization/deserialization)
    @Override
    public String toString() {
        return startDate + "@" + accountNumber + "@" + subject;
    }

    // Optional: JSON serialization using Jackson
    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    // Optional: JSON deserialization using Jackson
    public static CompositeKey fromJson(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, CompositeKey.class);
    }
}