package com.upthink;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import javax.mail.MessagingException;

public class Bootstrap {

    private static final String ACCOUNT_DETAILS_SPREADSHEET_ID = "11g3ManGTll90QFjT0oy1rXOVYyBLqh_R020fh13Ihak";
    private static final String BF_SCHEDULE_UPDATES_ID = "1lt5ureEp8LzvaiXV015RSNLjjW0w3Tt5kaEbV3qQ8ao";
    private GoogleSheetService accountDetailSpreadsheet;
    private GoogleSheetService scheduleSpreadSheet;
    private LocalDate yesterday;

    public Bootstrap() throws GeneralSecurityException, IOException {
        this.accountDetailSpreadsheet = new GoogleSheetService(ACCOUNT_DETAILS_SPREADSHEET_ID);
        this.scheduleSpreadSheet = new GoogleSheetService(BF_SCHEDULE_UPDATES_ID);
        this.yesterday = LocalDate.now().minusDays(1);
    }

    public GoogleSheetService getAccountDetailSpreadsheet() {
        return accountDetailSpreadsheet;
    }

    public GoogleSheetService getScheduleSpreadSheet() {
        return scheduleSpreadSheet;
    }

    public Map<String, Map<String, Object>> calculateSubjectHours(List<List<Object>> bfScheduleSheetData, List<List<Object>> bfPrevScheduleSheetData) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

        List<List<Object>> bfScheduleData = new ArrayList<>();
        for (List<Object> row : bfScheduleSheetData) {
            try {
                row.set(0, formatter.parse(row.get(0).toString()).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            bfScheduleData.add(row);
        }

        List<List<Object>> bfScheduleDataPrev = new ArrayList<>();
        for (List<Object> row : bfPrevScheduleSheetData) {
            try {
                LocalDate date = formatter.parse(row.get(0).toString()).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (date.isEqual(yesterday) || date.isAfter(yesterday)) {
                    row.set(0, formatter.parse(row.get(0).toString()).toString());
                    bfScheduleDataPrev.add(row);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Map<String, Double> subjectHours = new HashMap<>();
        for (List<Object> session : bfScheduleData) {
            String subject = session.get(3).toString();
            if (isNumeric(session.get(session.size() - 1).toString())) {
                subjectHours.put(subject, subjectHours.getOrDefault(subject, 0.0) + Double.parseDouble(session.get(session.size() - 1).toString()));
            }
        }

        Map<String, Double> subjectHoursPrev = new HashMap<>();
        for (List<Object> session : bfScheduleDataPrev) {
            String subject = session.get(3).toString();
            if (isNumeric(session.get(session.size() - 1).toString())) {
                subjectHoursPrev.put(subject, subjectHoursPrev.getOrDefault(subject, 0.0) + Double.parseDouble(session.get(session.size() - 1).toString()));
            }
        }

        Map<String, Map<String, Object>> subjectHoursCombined = new HashMap<>();
        for (String key : subjectHours.keySet()) {
            Map<String, Object> hours = new HashMap<>();
            hours.put("current_hours", subjectHours.getOrDefault(key, 0.0));
            hours.put("previous_hours", subjectHoursPrev.getOrDefault(key, 0.0));
            subjectHoursCombined.put(key, hours);
        }

        double totalCurrentHours = subjectHours.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalPreviousHours = subjectHoursPrev.values().stream().mapToDouble(Double::doubleValue).sum();

        Map<String, Object> totalHours = new HashMap<>();
        totalHours.put("current_hours", totalCurrentHours);
        totalHours.put("previous_hours", totalPreviousHours);

        subjectHoursCombined.put("Total", totalHours);

        return subjectHoursCombined;
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public Map<String, List<Map<String, String>>> getDifferencedData() throws IOException {
        ValueRange bfPrevScheduleRange = scheduleSpreadSheet.getRange(1, 1,
                scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
        List<List<Object>> bfPrevScheduleSheetData = bfPrevScheduleRange.getValues().subList(1, bfPrevScheduleRange.getValues().size());

        ValueRange bfScheduleRange = scheduleSpreadSheet.getRange(1, 1,
                scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
        List<List<Object>> bfScheduleSheetData = bfScheduleRange.getValues().subList(1, bfScheduleRange.getValues().size());

        List<List<Object>> currentValuesData = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        for (List<Object> row : bfScheduleSheetData) {
            try {
                row.set(0, formatter.parse(row.get(0).toString()).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            currentValuesData.add(row);
        }

        Map<String, Map<String, String>> currentDictionary = DataProcessor.createDictionary(currentValuesData);

        List<List<Object>> prevValuesData = new ArrayList<>();
        for (List<Object> row : bfPrevScheduleSheetData) {
            try {
                row.set(0, formatter.parse(row.get(0).toString()).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            prevValuesData.add(row);
        }

        Map<String, Map<String, String>> prevDictionary = DataProcessor.createDictionary(prevValuesData);

        return DataProcessor.compareDictionaries(prevDictionary, currentDictionary);
    }

    public static void main(String... args) throws GeneralSecurityException, IOException {
        Bootstrap bootstrap = new Bootstrap();
        GoogleSheetService accountDetailSpreadsheet = bootstrap.getAccountDetailSpreadsheet();
        Sheet sheet = accountDetailSpreadsheet.getSheetByName("All_Accts&Passwords");

        if (sheet != null) {
            ValueRange range = accountDetailSpreadsheet.getRange(1, 1,
                    accountDetailSpreadsheet.getLastRow(), accountDetailSpreadsheet.getLastColumn());
            List<List<Object>> values = range.getValues();
            values = values.subList(1, values.size());

            GoogleSheetService scheduleSpreadSheet = bootstrap.getScheduleSpreadSheet();

            Sheet sheetBFSchedule = scheduleSpreadSheet.getSheetByName("Copy of BF Schedule");
            if (sheetBFSchedule != null) {
                ValueRange rangeBFSchedule = scheduleSpreadSheet.getRange(1, 1,
                        scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
                List<List<Object>> currentBFScheduleValues = rangeBFSchedule.getValues().subList(1, rangeBFSchedule.getValues().size());

                Sheet previousBFScheduleSheet = scheduleSpreadSheet.getSheetByName("Copy of Previous_BF_Schedule");
                if (previousBFScheduleSheet != null) {
                    ValueRange rangePreviousBFSchedule = scheduleSpreadSheet.getRange(1, 1,
                            scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
                    List<List<Object>> previousBFScheduleValues = rangePreviousBFSchedule.getValues().subList(1, rangePreviousBFSchedule.getValues().size());
                    scheduleSpreadSheet.deleteRange(previousBFScheduleSheet, 2, 1, previousBFScheduleValues.size(), 11);
                    scheduleSpreadSheet.writeToSheet(previousBFScheduleSheet, 2, 1, currentBFScheduleValues);
                }
            }

            // Create a thread pool with 4 threads
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CompletionService<List<List<String>>> completionService = new ExecutorCompletionService<>(executor);

            // Submit web scraper tasks for each account
            for (List<Object> oneRow : values) {
                String username = (String) oneRow.get(0);
                String password = (String) oneRow.get(1);
                String subject = (String) oneRow.get(2);
                String singleDual = (String) oneRow.get(3);
                String audioCertified = (String) oneRow.get(4);
                WebScraperTask webScraper = new WebScraperTask(username, password, subject, singleDual, audioCertified);
                completionService.submit(webScraper);
            }

            // Process the results as they complete
            int totalTasks = values.size();
            int resultSize = 0;
            for (int i = 0; i < totalTasks; i++) {
                try {
                    Future<List<List<String>>> future = completionService.take();
                    List<List<String>> res = future.get();
                    List<List<Object>> result = new ArrayList<>();
                    for (List<String> row : res) {
                        List<Object> objectRow = new ArrayList<>(row);
                        result.add(objectRow);
                    }
                    scheduleSpreadSheet.writeToSheet(sheetBFSchedule, 2 + resultSize, 1, result);
                    resultSize += result.size();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Shut down the executor
            executor.shutdown();

            // Compare the two sheets and process the differences
            try {
                Map<String, List<Map<String, String>>> differencedData = bootstrap.getDifferencedData();

                List<String> changesInAccounts = differencedData.values().stream()
                        .flatMap(List::stream)
                        .map(map -> map.toString())
                        .distinct()
                        .collect(Collectors.toList());

                List<String> shiftAdded = new ArrayList<>();
                List<String> shiftDeleted = new ArrayList<>();
                List<String> shiftChanged = new ArrayList<>();

                SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
                for (Map<String, String> row : differencedData.values().stream().flatMap(List::stream).collect(Collectors.toList())) {
                    try {
                        LocalDate startDate = formatter.parse(row.get("Start Date")).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        String accountNum = row.get("Account Number");
                        String subject = row.get("Subject");
                        String key = startDate + "-" + accountNum;

                        if (differencedData.containsKey(key)) {
                            Map<String, String> currValues = differencedData.get(key).get(1);
                            Map<String, String> prevValues = differencedData.get(key).get(0);

                            String currValStartTime = currValues.get("Start Time");
                            String currValEndTime = currValues.get("End Time");
                            String prevValStartTime = prevValues.get("Start Time");
                            String prevValEndTime = prevValues.get("End Time");

                            if ((currValEndTime != null && !currValEndTime.equals("-")) || (currValStartTime != null && !currValStartTime.equals("-"))) {
                                if ((prevValEndTime == null || prevValEndTime.equals("-")) || (prevValStartTime == null || prevValStartTime.equals("-"))) {
                                    shiftAdded.add(key + " (Subject: " + subject + ")");
                                } else if ((currValEndTime == null || currValEndTime.equals("-")) && (currValStartTime == null || currValStartTime.equals("-"))) {
                                    shiftDeleted.add(key + " (Subject: " + subject + ")");
                                } else {
                                    shiftChanged.add(key + " (Subject: " + subject + ")");
                                }
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                String uniqueShiftAdded = String.join(", ", shiftAdded.stream().distinct().collect(Collectors.toList()));
                String uniqueShiftDeleted = String.join(", ", shiftDeleted.stream().distinct().collect(Collectors.toList()));
                String uniqueShiftChanged = String.join(", ", shiftChanged.stream().distinct().collect(Collectors.toList()));

                String shiftAddedStr = "New shift added for these accounts: " + uniqueShiftAdded;
                String shiftDeletedStr = "Shift deleted for these accounts: " + uniqueShiftDeleted;
                String shiftChangedStr = "Change in shift timings for these accounts: " + uniqueShiftChanged;

                String emailMessage = String.join("\n", shiftAddedStr, shiftDeletedStr, shiftChangedStr);

                GmailService gmailServiceTushar = new GmailService();
                String fromEmail = "automation@upthink.com";
                String toEmail = "sreenjay.sen@upthink.com";
                List<String> ccEmails = Arrays.asList("sreenjay.sen@upthink.com", "automation@upthink.com");
                String subject = "Test Email";
                String bodyText = emailMessage;

//                gmailServiceTushar.sendEmail(fromEmail, toEmail, ccEmails, subject, bodyText);


                // Calculate subject hours
                ValueRange bfScheduleRange = scheduleSpreadSheet.getRange(1, 1,
                        scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
                List<List<Object>> bfScheduleSheetData = bfScheduleRange.getValues().subList(1, bfScheduleRange.getValues().size());

                ValueRange bfPrevScheduleRange = scheduleSpreadSheet.getRange(1, 1,
                        scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
                List<List<Object>> bfPrevScheduleSheetData = bfPrevScheduleRange.getValues().subList(1, bfPrevScheduleRange.getValues().size());

                Map<String, Map<String, Object>> subjectHoursCombined = bootstrap.calculateSubjectHours(bfScheduleSheetData, bfPrevScheduleSheetData);
                System.out.println(subjectHoursCombined);
                // Process and display the results
                GmailService gmailServiceHours = new GmailService();
                Map<String, Object> templateVariables = new HashMap<>();
                templateVariables.put("subject_hours_combined", subjectHoursCombined);

                gmailServiceHours.sendEmailWithTemplate("automation@upthink.com",
                        "sreenjay.sen@upthink.com",
                        Arrays.asList("sreenjay.sen@upthink.com"),
                        "Monthly Hours Projections",
                        "Projected_Numbers.html",
                        templateVariables);
            } catch (GeneralSecurityException | IOException | MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
