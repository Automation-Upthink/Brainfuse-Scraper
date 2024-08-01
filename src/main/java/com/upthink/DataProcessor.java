package com.upthink;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProcessor {

    public static Map<String, Map<String, String>> createDictionary(List<List<Object>> data) {
        Map<String, Map<String, String>> uniqueData = new HashMap<>();
        for (List<Object> row : data) {
            String startDate = row.get(0).toString();
            String accountNumber = row.get(2).toString();
            String startTime = row.get(7).toString();
            String endTime = row.get(8).toString();
            String uniqueKey = startDate + "-" + accountNumber;
            if (!uniqueData.containsKey(uniqueKey)) {
                Map<String, String> timeMap = new HashMap<>();
                timeMap.put("Start Time", startTime);
                timeMap.put("End Time", endTime);
                uniqueData.put(uniqueKey, timeMap);
            }
        }
        return uniqueData;
    }

    public static Map<String, List<Map<String, String>>> compareDictionaries(Map<String, Map<String, String>> dict1, Map<String, Map<String, String>> dict2) {
        Map<String, List<Map<String, String>>> differences = new HashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm:ss a");

        for (Map.Entry<String, Map<String, String>> entry : dict1.entrySet()) {
            String key = entry.getKey();
            Map<String, String> values = entry.getValue();

            if (dict2.containsKey(key)) {
                String startTime1 = values.get("Start Time");
                String startTime2 = dict2.get(key).get("Start Time");
                String endTime1 = values.get("End Time");
                String endTime2 = dict2.get(key).get("End Time");
                LocalTime start1 = startTime1.equals("-") ? null : LocalTime.parse(startTime1.toLowerCase(), timeFormatter);
                LocalTime start2 = startTime2.equals("-") ? null : LocalTime.parse(startTime2.toLowerCase(), timeFormatter);
                LocalTime end1 = endTime1.equals("-") ? null : LocalTime.parse(endTime1.toLowerCase(), timeFormatter);
                LocalTime end2 = endTime2.equals("-") ? null : LocalTime.parse(endTime2.toLowerCase(), timeFormatter);

                if ((start1 != null && !start1.equals(start2)) || (end1 != null && !end1.equals(end2))) {
                    differences.put(key, List.of(values, dict2.get(key)));
                }
            }
        }
        return differences;
    }
}
