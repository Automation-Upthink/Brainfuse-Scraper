package com.upthink;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DataProcessor {

//    public static Map<String, Map<String, String>> createDictionary(List<List<Object>> data) {
//        Map<String, Map<String, String>> uniqueData = new HashMap<>();
//        for (List<Object> row : data) {
//            String startDate = row.get(0).toString();
//            String accountNumber = row.get(2).toString();
//            String startTime = row.get(7).toString();
//            String endTime = row.get(8).toString();
//            String uniqueKey = startDate + "@" + accountNumber;
//            if (!uniqueData.containsKey(uniqueKey)) {
//                Map<String, String> timeMap = new HashMap<>();
//                timeMap.put("Start Time", startTime);
//                timeMap.put("End Time", endTime);
//                uniqueData.put(uniqueKey, timeMap);
//            }
//        }
//        return uniqueData;
//    }

    public static Map<CompositeKey, Map<String, String>> createDictionary(List<List<Object>> data) throws JsonProcessingException {
        Map<CompositeKey, Map<String, String>> uniqueData = new HashMap<>();
        for(List<Object> row : data) {
            String startDate = row.get(0).toString();
            String accountNumber = row.get(2).toString();
            String subject = row.get(3).toString();
            String startTime = row.get(7).toString();
            String endTime = row.get(8).toString();
            CompositeKey uniqueKey = new CompositeKey(startDate, accountNumber, subject);
            if (!uniqueData.containsKey(uniqueKey)) {
                Map<String, String> timeMap = new HashMap<>();
                timeMap.put("Start Time", startTime);
                timeMap.put("End Time", endTime);
                uniqueData.put(uniqueKey, timeMap);
            }
        }
        return uniqueData;
    }

    public static Map<CompositeKey, List<Map<String, String>>> compareDictionaries(Map<CompositeKey, Map<String, String>> dict1, Map<CompositeKey, Map<String, String>> dict2) {
        Map<CompositeKey, List<Map<String, String>>> differences = new HashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);
        for (Map.Entry<CompositeKey, Map<String, String>> entry : dict1.entrySet()) {
            CompositeKey key = entry.getKey();
            Map<String, String> values = entry.getValue();
            if (dict2.containsKey(key)) {
                String startTimeToday = values.get("Start Time");
                String endTimeToday = values.get("End Time");
                LocalTime start1 = startTimeToday.equals("-") ? null : LocalTime.parse(startTimeToday, timeFormatter);
                LocalTime end1 = endTimeToday.equals("-") ? null : LocalTime.parse(endTimeToday, timeFormatter);

                String startTimeYesterday = dict2.get(key).get("Start Time");
                String endTimeYesterday = dict2.get(key).get("End Time");
                LocalTime start2 = startTimeYesterday.equals("-") ? null : LocalTime.parse(startTimeYesterday, timeFormatter);
                LocalTime end2 = endTimeYesterday.equals("-") ? null : LocalTime.parse(endTimeYesterday, timeFormatter);

                boolean startTimesDifferent = (start1 == null && start2 != null) || (start1 != null && !start1.equals(start2));
                boolean endTimesDifferent = (end1 == null && end2 != null) || (end1 != null && !end1.equals(end2));

                if (startTimesDifferent || endTimesDifferent) {
                    differences.put(key, List.of(values, dict2.get(key)));
                }
            }
        }
        return differences;
    }


    public static Map<CompositeKey, List<Map<String, String>>> getDifferencedData(List<List<Object>> arrayOfArray1, List<List<Object>> arrayOfArray2) throws JsonProcessingException {
        Map<CompositeKey, Map<String, String>> currentDictionary = DataProcessor.createDictionary(arrayOfArray1);
        Map<CompositeKey, Map<String, String>> prevDictionary = DataProcessor.createDictionary(arrayOfArray2);;
        return DataProcessor.compareDictionaries(currentDictionary, prevDictionary);
    }

    public static List<String> shiftModifications(Map<CompositeKey, List<Map<String, String>>> data) {
        List<String> shiftAdded = new ArrayList<>();
        List<String> shiftDeleted = new ArrayList<>();
        List<String> shiftChanged = new ArrayList<>();

        for(Map.Entry<CompositeKey, List<Map<String, String>>> entry: data.entrySet()) {
            CompositeKey key = entry.getKey();
            List<Map<String, String>> values = entry.getValue();
            Map<String, String> todayValue = values.get(0);
            Map<String, String> yesterdayValue = values.get(1);
            String currValStartTime = todayValue.get("Start Time");
            String currValEndTime = todayValue.get("End Time");
            String prevValStartTime = yesterdayValue.get("Start Time");
            String prevValEndTime = yesterdayValue.get("End Time");

            if (currValStartTime.equals("-") || currValEndTime.equals("-")) {
                shiftDeleted.add("(Subject: "+key.getSubject()+" "+key.getAccountNumber()+ ")");
            } else if (prevValStartTime.equals("-") && prevValEndTime.equals("-")) {
                shiftAdded.add("(Subject: "+key.getSubject()+" "+key.getAccountNumber()+ ")");
            } else {
                shiftChanged.add("(Subject: "+key.getSubject()+" "+key.getAccountNumber()+ ")");
            }
        }

        String uniqueShiftAdded = String.join(", ", shiftAdded.stream().distinct().collect(Collectors.toList()));
        String uniqueShiftDeleted = String.join(", ", shiftDeleted.stream().distinct().collect(Collectors.toList()));
        String uniqueShiftChanged = String.join(", ", shiftChanged.stream().distinct().collect(Collectors.toList()));

        String shiftAddedStr = "New shift added for these accounts: " + uniqueShiftAdded;
        String shiftDeletedStr = "Shift deleted for these accounts: " + uniqueShiftDeleted;
        String shiftChangedStr = "Change in shift timings for these accounts: " + uniqueShiftChanged;

        return List.of(shiftAddedStr, shiftDeletedStr, shiftChangedStr);
//        return String.join("\n", shiftAddedStr, shiftDeletedStr, shiftChangedStr);
    }

    private static List<List<Object>> objectToString(List<List<Object>> obj) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        List<List<Object>> stringListOfLists = new ArrayList<>();
        for(List<Object> row : obj) {
            row.set(0, dateFormatter.parse(row.get(0).toString()).toString());
            stringListOfLists.add(row);
        }
        return stringListOfLists;
    }

    private static Map<String, Double> createMap(List<List<Object>> listOfLists) {
        Map<String, Double> mapper = new HashMap<>();
        for(List<Object> row : listOfLists) {
            String subject = row.get(3).toString();
            if (isNumeric(row.get(row.size() - 1).toString())) {
                mapper.put(subject, mapper.getOrDefault(subject, 0.0) + Double.parseDouble(row.get(row.size() - 1).toString()));
            }
        }
        return mapper;
    }

    public static Map<String, Map<String, Double>> compareSubjectHours(List<List<Object>> currentBFData, List<List<Object>> previousBFData) {
        Map<String, Double> currentBFMap = calculateSubjectHours(currentBFData);
        Map<String, Double> previousBFMap = calculateSubjectHours(previousBFData);

        // Initialize the result map
        Map<String, Map<String, Double>> comparisonMap = new HashMap<>();

        // Variables to track total hours
        double totalCurrentHours = 0.0;
        double totalPreviousHours = 0.0;

        // Combine subjects from both maps
        Set<String> allSubjects = new HashSet<>();
        allSubjects.addAll(currentBFMap.keySet());
        allSubjects.addAll(previousBFMap.keySet());
        // Build the comparison map
        for (String subject : allSubjects) {
            double currentHours = currentBFMap.getOrDefault(subject, 0.0);
            double previousHours = previousBFMap.getOrDefault(subject, 0.0);
            // Add totals
            totalCurrentHours += currentHours;
            totalPreviousHours += previousHours;
            // Create a nested map for the subject
            Map<String, Double> hoursMap = new HashMap<>();
            hoursMap.put("previous_hours", previousHours);
            hoursMap.put("current_hours", currentHours);
            // Add the nested map to the comparison map
            comparisonMap.put(subject, hoursMap);
        }
        return comparisonMap;
    }


    private static Map<String, Double> calculateSubjectHours(List<List<Object>> brainfuseScraperData) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
        Map<String, Double> subjectHoursMap = new HashMap<>();
        // Transform data and populate the map
        brainfuseScraperData.stream()
                .filter(row -> row.size() > 10 && !"-".equals(row.get(10).toString()))
                .forEach(row -> {
                    // Parse the last element as Double (if it's a String)
                    Double subjectHours = 0.0;
                    try {
                        subjectHours = Double.parseDouble(row.get(10).toString());
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse subject hours: " + row.get(10));
                    }

//                    // Convert the first column to LocalDate
//                    if (row.get(0) instanceof String) {
//                        String dateString = (String) row.get(0);
//                        try {
//                            LocalDate parsedDate = LocalDate.parse(dateString, dtf);
//                            row.set(0, parsedDate); // Replace the string with LocalDate
//                        } catch (Exception e) {
//                            System.err.println("Failed to parse date string: " + dateString);
//                        }
//                    }
                    // Calculate subject hours
                    String subject = (String) row.get(3); // Assuming the subject is in column 4 (0-based index 3)
                    Double hours = Double.parseDouble(row.get(10).toString()); // Assuming hours are in column 11 (index 10)
                    // Accumulate hours in the map
                    subjectHoursMap.put(subject, subjectHoursMap.getOrDefault(subject, 0.0) + hours);
                    // Return the updated row
                });
        return subjectHoursMap;
    }


    public static List<List<Object>> dateFormattedData(List<List<Object>> data) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

        return data.stream()
                .map(row -> {
                    List<Object> newRow = new ArrayList<>(row); // Create a copy of the row
                    if (!newRow.isEmpty() && newRow.get(0) != null) {
                        String dateString = newRow.get(0).toString(); // Get the first element as a string
                        try {
                            LocalDate parsedDate = LocalDate.parse(dateString, dtf);
                            newRow.set(0, parsedDate); // Replace the first element with LocalDate
                        } catch (Exception e) {
                            System.err.println("Failed to parse date string: " + dateString);
                        }
                    }
                    return newRow;
                })
                .collect(Collectors.toList());
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
