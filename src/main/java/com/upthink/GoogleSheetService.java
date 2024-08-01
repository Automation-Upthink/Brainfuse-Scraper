package com.upthink;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import com.google.api.services.sheets.v4.model.Sheet;


public class GoogleSheetService {

    private String SPREADSHEET_ID;
    private Sheets SERVICE;
    private Sheet SHEET;
    private Spreadsheet SPREADSHEET;

    public GoogleSheetService(String spreadSheetId) throws GeneralSecurityException, IOException {
        this.SPREADSHEET_ID = spreadSheetId;
        this.SERVICE = GoogleAuthentication.build(); // Initialize the Sheets service
        this.SPREADSHEET = SERVICE.spreadsheets().get(SPREADSHEET_ID).execute();
    }

    public Sheet getSheetByName(String sheetName) {
        List<Sheet> sheets = SPREADSHEET.getSheets();
        for (Sheet sheet : sheets) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                this.SHEET = sheet;
                return sheet;
            }
        }
        return null; // Return null if no sheet with the given name is found
    }

    public ValueRange getRange(int startRow, int startColumn, int numRows, int numColumns)
            throws IOException {
        if (SHEET == null) {
            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
        }
        String sheetName = SHEET.getProperties().getTitle();
        String startColumnLetter = columnNumberToLetter(startColumn);
        String endColumnLetter = columnNumberToLetter(startColumn + numColumns - 1);
        String range = String.format("%s!%s%d:%s%d", sheetName, startColumnLetter, startRow, endColumnLetter, startRow + numRows - 1);
        return SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
    }

    public void writeToSheet(Sheet sheet, int startRow, int startColumn, List<List<Object>> values) throws IOException {
        if (sheet == null) {
            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
        }
        String sheetName = sheet.getProperties().getTitle();
        String startColumnLetter = columnNumberToLetter(startColumn);
        String endColumnLetter = columnNumberToLetter(startColumn + values.get(0).size() - 1);
        String range = String.format("%s!%s%d:%s%d", sheetName, startColumnLetter, startRow, endColumnLetter, startRow + values.size() - 1);

        ValueRange body = new ValueRange().setValues(values);
        SERVICE.spreadsheets().values().update(SPREADSHEET_ID, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    public void deleteRange(Sheet sheet, int startRow, int startColumn, int numRows, int numColumns) throws IOException {
        int sheetId = sheet.getProperties().getSheetId();

        // Fetch the range of values to determine actual end row and column
        String startColumnLetter = columnNumberToLetter(startColumn);
        String endColumnLetter = columnNumberToLetter(startColumn + numColumns - 1);
        String range = String.format("%s!%s%d:%s%d", sheet.getProperties().getTitle(), startColumnLetter, startRow, endColumnLetter, startRow + numRows - 1);
        ValueRange valueRange = SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();

        List<List<Object>> values = valueRange.getValues();
        if (values == null || values.isEmpty()) {
            return; // No values to delete
        }

        int actualEndRow = startRow;
        int actualEndColumn = startColumn;

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row != null && !row.isEmpty()) {
                actualEndRow = startRow + i;
                for (int j = 0; j < row.size(); j++) {
                    if (row.get(j) != null && !row.get(j).toString().isEmpty()) {
                        actualEndColumn = Math.max(actualEndColumn, startColumn + j);
                    }
                }
            }
        }

        // Create the list of requests to be executed
        List<Request> requests = new ArrayList<>();
        requests.add(new Request()
                .setDeleteRange(new DeleteRangeRequest()
                        .setRange(new GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(startRow - 1)
                                .setEndRowIndex(actualEndRow) // End row adjusted to actual last row with data
                                .setStartColumnIndex(startColumn - 1)
                                .setEndColumnIndex(actualEndColumn)) // End column adjusted to actual last column with data
                        .setShiftDimension("ROWS"))); // Shift rows up after deletion

        // Create the BatchUpdateSpreadsheetRequest
        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);

        // Execute the batch update
        SERVICE.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();
    }


    public int getLastRow() throws IOException {
        if (SHEET == null) {
            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
        }
        String sheetName = SHEET.getProperties().getTitle();
        String range = sheetName + "!A:A";
        ValueRange response = SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
        List<List<Object>> values = response.getValues();
        return values != null ? values.size() : 0;
    }

    public int getLastColumn() throws IOException {
        if (SHEET == null) {
            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
        }
        String sheetName = SHEET.getProperties().getTitle();
        String range = sheetName + "!1:1";
        ValueRange response = SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
        List<List<Object>> values = response.getValues();
        return values != null && !values.isEmpty() ? values.get(0).size() : 0;
    }

    private static String columnNumberToLetter(int columnNumber) {
        StringBuilder columnLetter = new StringBuilder();
        while (columnNumber > 0) {
            int remainder = (columnNumber - 1) % 26;
            columnLetter.insert(0, (char)(remainder + 'A'));
            columnNumber = (columnNumber - 1) / 26;
        }
        return columnLetter.toString();
    }

}

//import com.google.api.services.sheets.v4.Sheets;
//import com.google.api.services.sheets.v4.model.*;
//import com.google.api.services.sheets.v4.model.Sheet;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class GoogleSheetService {
//
//    private String SPREADSHEET_ID;
//    private static String writeSpreadsheetId;
//    private static Sheets SERVICE;
//    private static String currentSheetName;
//    private static Sheet SHEET;
//    private static Spreadsheet SPREADSHEET;
//
//    public GoogleSheetService(String spreadSheetId) throws GeneralSecurityException, IOException {
//        this.SPREADSHEET_ID = spreadSheetId;
//        SERVICE = GoogleAuthentication.build(); // Initialize the Sheets service
//        SPREADSHEET = SERVICE.spreadsheets().get(SPREADSHEET_ID).execute();
//    }
//
//    public Sheet getSheetByName(String sheetName) {
//        List<Sheet> sheets = SPREADSHEET.getSheets();
//        for (Sheet sheet : sheets) {
//            if (sheet.getProperties().getTitle().equals(sheetName)) {
//                SHEET = sheet;
//            }
//        }
//        return null; // Return null if no sheet with the given name is found
//    }
//
//    public ValueRange getRange(int startRow, int startColumn, int numRows, int numColumns)
//            throws IOException {
//        if (SHEET == null) {
//            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
//        }
//        String sheetName = SHEET.getProperties().getTitle();
//        String startColumnLetter = columnNumberToLetter(startColumn);
//        String endColumnLetter = columnNumberToLetter(startColumn + numColumns - 1);
//        String range = String.format("%s!%s%d:%s%d", sheetName, startColumnLetter, startRow, endColumnLetter, startRow + numRows - 1);
//        return SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
//    }
//
//    public void writeToSheet(Sheet sheet, int startRow, int startColumn, List<List<Object>> values) throws IOException {
//        if (sheet == null) {
//            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
//        }
//        String sheetName = sheet.getProperties().getTitle();
//        String startColumnLetter = columnNumberToLetter(startColumn);
//        String endColumnLetter = columnNumberToLetter(startColumn + values.get(0).size() - 1);
//        String range = String.format("%s!%s%d:%s%d", sheetName, startColumnLetter, startRow, endColumnLetter, startRow + values.size() - 1);
//
//        // Convert List<List<String>> to List<List<Object>>
////        List<List<Object>> objectValues = new ArrayList<>();
////        for (List<String> row : values) {
////            List<Object> objectRow = new ArrayList<>(row);
////            objectValues.add(objectRow);
////        }
//        ValueRange body = new ValueRange().setValues(values);
//        UpdateValuesResponse response = SERVICE.spreadsheets().values().update(SPREADSHEET_ID, range, body)
//                .setValueInputOption("USER_ENTERED") // or "USER_ENTERED" based on your needs
//                .execute();
//    }
//
//    public void deleteRange(Sheet sheet, int startRow, int startColumn, int numberOfRows) throws IOException {
//
//        int sheetId = sheet.getProperties().getSheetId();
//
//        // Create the list of requests to be executed
//        List<Request> requests = new ArrayList<>();
//
//        // Create the DeleteRangeRequest for the specified range
//        requests.add(new Request()
//                .setDeleteRange(new DeleteRangeRequest()
//                        .setRange(new GridRange()
//                                .setSheetId(sheetId)
//                                .setStartRowIndex(startRow - 1) // Adjusting for zero-based index
//                                .setEndRowIndex(startRow - 1 + numberOfRows) // End row is start row + number of rows
//                                .setStartColumnIndex(startColumn - 1) // Adjusting for zero-based index
//                                .setEndColumnIndex(startColumn)) // End column is start column + 1 (single column deletion)
//                        .setShiftDimension("ROWS"))); // Shift rows up after deletion
//
//        // Create the BatchUpdateSpreadsheetRequest
//        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
//
//        // Execute the batch update
//        SERVICE.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();
//    }
//
//
//
//    public int getLastRow() throws IOException {
//        if (SHEET == null) {
//            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
//        }
//        String sheetName = SHEET.getProperties().getTitle();
//        String range = sheetName + "!A:A";
//        ValueRange response = SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
//        List<List<Object>> values = response.getValues();
//        return values != null ? values.size() : 0;
//    }
//
//    public int getLastColumn() throws IOException {
//        if (SHEET == null) {
//            throw new IllegalStateException("Sheet is not set. Call getSheetByName first.");
//        }
//        String sheetName = SHEET.getProperties().getTitle();
//        String range = sheetName + "!1:1";
//        ValueRange response = SERVICE.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
//        List<List<Object>> values = response.getValues();
//        return values != null && !values.isEmpty() ? values.get(0).size() : 0;
//    }
//
//    private static String columnNumberToLetter(int columnNumber) {
//        StringBuilder columnLetter = new StringBuilder();
//        while (columnNumber > 0) {
//            int remainder = (columnNumber - 1) % 26;
//            columnLetter.insert(0, (char)(remainder + 'A'));
//            columnNumber = (columnNumber - 1) / 26;
//        }
//        return columnLetter.toString();
//    }
//
//}