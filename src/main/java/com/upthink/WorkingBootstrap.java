//package com.upthink;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.sql.Array;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletionService;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorCompletionService;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//
//import com.google.api.services.sheets.v4.model.Sheet;
//import com.google.api.services.sheets.v4.model.Spreadsheet;
//import com.google.api.services.sheets.v4.model.ValueRange;
//import com.upthink.Objects.CalendarObject;
//import com.upthink.Objects.Pair;
//
//import javax.mail.MessagingException;
//
//public class Bootstrap {
//
//    private static final String ACCOUNT_DETAILS_SPREADSHEET_ID = "11g3ManGTll90QFjT0oy1rXOVYyBLqh_R020fh13Ihak";
//    private static final String BF_SCHEDULE_UPDATES_ID = "1lt5ureEp8LzvaiXV015RSNLjjW0w3Tt5kaEbV3qQ8ao";
//    private GoogleSheetService accountDetailSpreadsheet;
//    private GoogleSheetService scheduleSpreadSheet;
//
//
//    public Bootstrap() throws GeneralSecurityException, IOException {
//        this.accountDetailSpreadsheet = new GoogleSheetService(ACCOUNT_DETAILS_SPREADSHEET_ID);
//        this.scheduleSpreadSheet = new GoogleSheetService(BF_SCHEDULE_UPDATES_ID);
//    }
//
//    public GoogleSheetService getAccountDetailSpreadsheet() {
//        return accountDetailSpreadsheet;
//    }
//
//    public GoogleSheetService getScheduleSpreadSheet() {
//        return scheduleSpreadSheet;
//    }
//
//    public Map<String, List<Map<String, String>>> getDifferencedData() throws IOException {
//        ValueRange bfPrevScheduleRange = scheduleSpreadSheet.getRange(1, 1,
//                scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
//        List<List<Object>> bfPrevScheduleSheetData = bfPrevScheduleRange.getValues().subList(1, bfPrevScheduleRange.getValues().size());
//
//        ValueRange bfScheduleRange = scheduleSpreadSheet.getRange(1, 1,
//                scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
//        List<List<Object>> bfScheduleSheetData = bfScheduleRange.getValues().subList(1, bfScheduleRange.getValues().size());
//
//        List<List<Object>> currentValuesData = new ArrayList<>();
//        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
//        for (List<Object> row : bfScheduleSheetData) {
//            try {
//                row.set(0, formatter.parse(row.get(0).toString()).toString());
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//            currentValuesData.add(row);
//        }
//
//        Map<String, Map<String, String>> currentDictionary = DataProcessor.createDictionary(currentValuesData);
//
//        List<List<Object>> prevValuesData = new ArrayList<>();
//        for (List<Object> row : bfPrevScheduleSheetData) {
//            try {
//                row.set(0, formatter.parse(row.get(0).toString()).toString());
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//            prevValuesData.add(row);
//        }
//
//        Map<String, Map<String, String>> prevDictionary = DataProcessor.createDictionary(prevValuesData);
//
//        return DataProcessor.compareDictionaries(prevDictionary, currentDictionary);
//    }
//
//    public static void main(String... args) throws GeneralSecurityException, IOException {
//        Bootstrap bootstrap = new Bootstrap();
//        GoogleSheetService accountDetailSpreadsheet = bootstrap.getAccountDetailSpreadsheet();
//        Sheet sheet = accountDetailSpreadsheet.getSheetByName("All_Accts&Passwords");
//
//        if (sheet != null) {
//            ValueRange range = accountDetailSpreadsheet.getRange(1, 1,
//                    accountDetailSpreadsheet.getLastRow(), accountDetailSpreadsheet.getLastColumn());
//            List<List<Object>> values = range.getValues();
//            values = values.subList(1, 4);
//
//            GoogleSheetService scheduleSpreadSheet = bootstrap.getScheduleSpreadSheet();
//
//            Sheet sheetBFSchedule = scheduleSpreadSheet.getSheetByName("Copy of BF Schedule");
//            if (sheetBFSchedule != null) {
//                ValueRange rangeBFSchedule = scheduleSpreadSheet.getRange(1, 1,
//                        scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
//                List<List<Object>> currentBFScheduleValues = rangeBFSchedule.getValues().subList(1, rangeBFSchedule.getValues().size());
//
//                Sheet previousBFScheduleSheet = scheduleSpreadSheet.getSheetByName("Copy of Previous_BF_Schedule");
//                if (previousBFScheduleSheet != null) {
//                    ValueRange rangePreviousBFSchedule = scheduleSpreadSheet.getRange(1, 1,
//                            scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
//                    List<List<Object>> previousBFScheduleValues = rangePreviousBFSchedule.getValues().subList(1, rangePreviousBFSchedule.getValues().size());
//                    scheduleSpreadSheet.deleteRange(previousBFScheduleSheet, 2, 1, previousBFScheduleValues.size(), 11);
//                    scheduleSpreadSheet.writeToSheet(previousBFScheduleSheet, 2, 1, currentBFScheduleValues);
//                }
//                scheduleSpreadSheet.deleteRange(sheetBFSchedule, 2, 1, currentBFScheduleValues.size(), 11);
//            }
//
//            // Create a thread pool with 4 threads
//            ExecutorService executor = Executors.newFixedThreadPool(4);
//            CompletionService<List<List<String>>> completionService = new ExecutorCompletionService<>(executor);
//
//            // Submit web scraper tasks for each account
//            for (List<Object> oneRow : values) {
//                String username = (String) oneRow.get(0);
//                String password = (String) oneRow.get(1);
//                String subject = (String) oneRow.get(2);
//                String singleDual = (String) oneRow.get(3);
//                String audioCertified = (String) oneRow.get(4);
//                WebScraperTask webScraper = new WebScraperTask(username, password, subject, singleDual, audioCertified);
//                completionService.submit(webScraper);
//            }
//
//            // Process the results as they complete
//            int totalTasks = values.size();
//            int resultSize = 0;
//            for (int i = 0; i < totalTasks; i++) {
//                try {
//                    Future<List<List<String>>> future = completionService.take();
//                    List<List<String>> res = future.get();
//                    List<List<Object>> result = new ArrayList<>();
//                    for (List<String> row : res) {
//                        List<Object> objectRow = new ArrayList<>(row);
//                        result.add(objectRow);
//                    }
//                    scheduleSpreadSheet.writeToSheet(sheetBFSchedule, 2 + resultSize, 1, result);
//                    resultSize += result.size();
//                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            // Shut down the executor
//            executor.shutdown();
//
//            // Get the data from BF Schedule since new data has been written.
//            Sheet newSheetBFSchedule = scheduleSpreadSheet.getSheetByName("Copy of BF Schedule");
//            if (newSheetBFSchedule != null) {
//                ValueRange rangeBFSchedule = scheduleSpreadSheet.getRange(1, 1,
//                        scheduleSpreadSheet.getLastRow(), scheduleSpreadSheet.getLastColumn());
//                List<List<Object>> currentBFScheduleValues = rangeBFSchedule.getValues().subList(1, rangeBFSchedule.getValues().size());
//
//                // Compare the two sheets
//
//            }
//
//
//            try {
//
//                Map<String, List<Map<String, String>>> differencedData = bootstrap.getDifferencedData();
//
//                GmailService gmailService = new GmailService();
//                String fromEmail = "automation@upthink.com";
//                String toEmail = "recipient-email@gmail.com";
//                List<String> ccEmails = Arrays.asList("sreenjay.sen@upthink.com", "automation@upthink.com");
//                String subject = "Test Email";
//                String bodyText = "This is a test email with multiple CC addresses.";
//
//                gmailService.sendEmail("automation@upthink.com",
//                        toEmail,
//                        ccEmails,
//                        subject,
//                        bodyText);
//            } catch (GeneralSecurityException | IOException | MessagingException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//}
