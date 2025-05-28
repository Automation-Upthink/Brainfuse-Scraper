package com.upthink;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


import com.google.api.services.gmail.Gmail;
import com.google.api.services.sheets.v4.Sheets;
import com.google.integration.GMailService;
import com.google.integration.GoogleAuthentication;
import com.google.integration.Spreadsheet;
import com.google.integration.Spreadsheet.Sheet;

import io.github.cdimascio.dotenv.Dotenv;

import javax.mail.MessagingException;


public class Bootstrap {

    private static final String ACCOUNT_DETAILS_SPREADSHEET_ID = "11g3ManGTll90QFjT0oy1rXOVYyBLqh_R020fh13Ihak";
    private static final String BF_SCHEDULE_UPDATES_ID =  "1lt5ureEp8LzvaiXV015RSNLjjW0w3Tt5kaEbV3qQ8ao"; //"1lt5ureEp8LzvaiXV015RSNLjjW0w3Tt5kaEbV3qQ8ao";
    private static final String ENV_GMAIL_CLIENT_SECRET_JSON = "OAUTH2_CREDENTIALS_FILE";
    private static final Object writeLock = new Object();
    private final LocalDate yesterday = LocalDate.now().minusDays(1);

    public Bootstrap() throws GeneralSecurityException, IOException {
        getSpreadsheetService();
        getGmailService();
        Spreadsheet scaperSpreadsheet = Spreadsheet.openById(BF_SCHEDULE_UPDATES_ID);

        Sheet bfSchedulesheet = scaperSpreadsheet.getSheetByName("BF Schedule");


        Sheet previousBfSchedulesheet = scaperSpreadsheet.getSheetByName("Previous BF Schedule");

        // Web scrape the bf accounts
        // scrapeBrainfuse(bfSchedulesheet, previousBfSchedulesheet, 4);
        // Compare today's and yesterday's schedules
        compareAndEmail(bfSchedulesheet, previousBfSchedulesheet);
    }


    private <T> T safeCast(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)){
            return (T)obj;
        } else {
            return null;
        }
    }


    private void compareAndEmail(Sheet todayBFSheet, Sheet yesterdayBFSheet) throws IOException {
        List<List<Object>> bfScheduleValues = todayBFSheet
                .getRange(2, 1, todayBFSheet.getLastRow(), todayBFSheet.getLastColumn())
                .getValues();
        List<List<Object>> previousBfScheduleValues = yesterdayBFSheet
                .getRange(2, 1, yesterdayBFSheet.getLastRow()-1, yesterdayBFSheet.getLastColumn())
                .getValues();

        Map<String, Map<String, Double>> comparedSubjectHours = DataProcessor.compareSubjectHours(bfScheduleValues, previousBfScheduleValues);

        List<List<Object>> formattedBfScheduleValues = DataProcessor.dateFormattedData(bfScheduleValues);
        List<List<Object>> formattedPreviousBfScheduleValues = DataProcessor.dateFormattedData(previousBfScheduleValues);

        // Compare the two sheets and process the differences
        try {
            Map<CompositeKey, List<Map<String, String>>> differencedData = DataProcessor.getDifferencedData(formattedBfScheduleValues, formattedPreviousBfScheduleValues);

            // Initialize lists to store categorized shifts
            List<String> shiftAdded = new ArrayList<>();
            List<String> shiftDeleted = new ArrayList<>();
            List<String> shiftChanged = new ArrayList<>();

            for (Map.Entry<CompositeKey, List<Map<String, String>>> entry : differencedData.entrySet()) {
                CompositeKey key = entry.getKey(); // Get the key (CompositeKey object)
                List<Map<String, String>> value = entry.getValue(); // Get the value (List of Maps)

                String accountNumber = key.getAccountNumber();
                String subject = key.getSubject();

                // Retrieve current and previous shift values
                Map<String, String> currValues = value.get(0);
                Map<String, String> prevValues = value.get(1);

                String currValStartTime = currValues.get("Start Time");
                String currValEndTime = currValues.get("End Time");
                String prevValStartTime = prevValues.get("Start Time");
                String prevValEndTime = prevValues.get("End Time");

                if ((prevValEndTime == null || prevValEndTime.equals("-") || prevValStartTime == null || prevValStartTime.equals("-")) &&
                        (currValEndTime != null && !currValEndTime.equals("-") && currValStartTime != null && !currValStartTime.equals("-"))) {
                    shiftAdded.add(accountNumber + " (Subject: " + subject + ")");
                } else if ((currValEndTime == null || currValEndTime.equals("-") || currValStartTime == null || currValStartTime.equals("-")) &&
                        (prevValEndTime != null && !prevValEndTime.equals("-") && prevValStartTime != null && !prevValStartTime.equals("-"))) {
                    shiftDeleted.add(accountNumber + " (Subject: " + subject + ")");
                } else {
                    shiftChanged.add(accountNumber + " (Subject: " + subject + ")");
                }

            }

            // Generate unique and concatenated shift strings
            String uniqueShiftAdded = String.join(", ", shiftAdded.stream().distinct().collect(Collectors.toList()));
            String uniqueShiftDeleted = String.join(", ", shiftDeleted.stream().distinct().collect(Collectors.toList()));
            String uniqueShiftChanged = String.join(", ", shiftChanged.stream().distinct().collect(Collectors.toList()));

            String shiftAddedStr = "New shift added for these accounts: " + uniqueShiftAdded;
            String shiftDeletedStr = "Shift deleted for these accounts: " + uniqueShiftDeleted;
            String shiftChangedStr = "Change in shift timings for these accounts: " + uniqueShiftChanged;

            String emailMessage = "<p>" + shiftAddedStr + "</p>"
                    + "<p>" + shiftDeletedStr + "</p>"
                    + "<p>" + shiftChangedStr + "</p>";

            // Output the email message (for verification)
            System.out.println("Email Message:\n" + emailMessage);
            GMailService emailService = new GMailService();
            String fromEmail = "automation@upthink.com";
            String toEmail = "sreenjay.sen@upthink.com" //"tushar.jangale@upthink.com";
            List<String> ccEmails = Arrays.asList("sreenjay.sen@upthink.com");  //, "tejas.jagtap@upthink.com"
            String subject = "BF Shift changes";
            String bodyText = emailMessage;

            emailService.sendEmails(fromEmail, toEmail, ccEmails, subject, bodyText);

            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("subject_hours_combined", comparedSubjectHours);

            emailService.sendEmails("automation@upthink.com",
                    "sreenjay.sen@upthink.com", //"apurva.yadav@upthink.com",
                    Arrays.asList("sreenjay.sen@upthink.com"), //, "tejas.jagtap@upthink.com"
                    "Monthly Hours Projections",
                    "Projected_Numbers.html",
                    templateVariables);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    private void scrapeBrainfuse(Sheet bfSchedulesheet, Sheet previousBfSchedulesheet, int threads) throws IOException {
        List<List<Object>> bfScheduleValues = bfSchedulesheet
                .getRange(2, 1, bfSchedulesheet.getLastRow()-1, bfSchedulesheet.getLastColumn())
                .getValues();
        previousBfSchedulesheet
                .getRange(2, 1, previousBfSchedulesheet.getLastRow(), previousBfSchedulesheet.getLastColumn()).clear();
        previousBfSchedulesheet
                .getRange(2, 1, bfScheduleValues.size(), bfScheduleValues.get(0).size()).setValues(bfScheduleValues);

        bfSchedulesheet
                .getRange(2, 1, bfSchedulesheet.getLastRow(), bfSchedulesheet.getLastColumn()).clear();

        Spreadsheet accountDetailsSpreadsheet = Spreadsheet.openById(ACCOUNT_DETAILS_SPREADSHEET_ID);
        // Do things...
        Sheet passwordSheet = accountDetailsSpreadsheet.getSheetByName("All_Accts&Passwords");
        List<List<Object>> passwordSheetValues = passwordSheet
                .getRange(1, 1, passwordSheet.getLastRow(), passwordSheet.getLastColumn())
                .getValues();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CompletionService<List<List<String>>> completionService = new ExecutorCompletionService<>(executor);

        List<WebScraperTask> tasks = passwordSheetValues.stream()
                .skip(1)
                .filter(oneRow -> oneRow.size() >= 5)
                .map(oneRow -> {
                    String username = safeCast(oneRow.get(0), String.class);
                    String password = safeCast(oneRow.get(1), String.class);
                    String subject = safeCast(oneRow.get(2), String.class);
                    String singleDual = safeCast(oneRow.get(3), String.class);
                    String audioCertified = safeCast(oneRow.get(4), String.class);
                    return new WebScraperTask(username, password, subject, singleDual, audioCertified);
                }).collect(Collectors.toList());


        // Submit
        for (WebScraperTask task : tasks) {
            completionService.submit(task);
        }

        // Process the results as they complete
        int startRow = 2; // Start writing from row 2 since row 1 is the header
        for (int i = 0; i < tasks.size(); i++) {
            try {
                Future<List<List<String>>> future = completionService.take();
                List<List<String>> res = future.get();
                List<List<Object>> result = new ArrayList<>();
                // Convert String rows to Object rows
                for (List<String> row : res) {
                    result.add(new ArrayList<>(row));
                }
                int numRows = result.size(); // Number of rows in the current batch

                synchronized (writeLock) {
                    // We now pass the row count (numRows) and column count (lastCol)
                    int lastCol = bfSchedulesheet.getLastColumn();  // e.g. 10 if column J is the last non-empty col
                    String range = bfSchedulesheet
                            .getRange(startRow, 1, numRows, lastCol)
                            .getA1Notation();

                    // Actually write to the sheet (uncomment in real code)
                    bfSchedulesheet
                            .getRange(startRow, 1, numRows, lastCol)
                            .setValues(result);

                    // Move the start row down for the next batch
                    if (numRows > 0) {
                        startRow += numRows;
                    }
                }

            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }
        }

        // Shut down the executor
        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) { // Wait for 60 seconds
                executor.shutdownNow(); // Force shutdown if not terminated
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Executor did not terminate properly!");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow(); // Re-interrupt if current thread is interrupted
            Thread.currentThread().interrupt();
        }
    }


    public static void main(String []args) throws GeneralSecurityException, IOException {
        Bootstrap bootstrap = new Bootstrap();
    }


    private void getSpreadsheetService() {
        String credentialFilePath = System.getenv(ENV_GMAIL_CLIENT_SECRET_JSON);
        if (credentialFilePath == null || credentialFilePath.isEmpty()){
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't throw an error if .env is missing
                    .load();
            credentialFilePath = dotenv.get(ENV_GMAIL_CLIENT_SECRET_JSON);
        }
        try {
            Sheets service = GoogleAuthentication.buildSheetsServiceWithOAuth2Base64(credentialFilePath);
            Spreadsheet.initializeService(service);

        } catch (GeneralSecurityException | IOException  e) {
            throw new RuntimeException(e);
        }
    }


    private void getGmailService() {
        String credentialFilePath = System.getenv(ENV_GMAIL_CLIENT_SECRET_JSON);
        if (credentialFilePath == null || credentialFilePath.isEmpty()) {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't throw an error if .env is missing
                    .load();
            credentialFilePath = dotenv.get(ENV_GMAIL_CLIENT_SECRET_JSON);
        }
        try {
            Gmail service = GoogleAuthentication.buildGmailServiceWithOAuth2Base64(credentialFilePath);
            GMailService.initializeService(service); // Initialize or use the service as needed
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }


}



