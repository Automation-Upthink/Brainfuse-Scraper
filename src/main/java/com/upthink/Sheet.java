package com.upthink;

public class Sheet {

    private com.google.api.services.sheets.v4.model.Sheet sheet;

    public Sheet(com.google.api.services.sheets.v4.model.Sheet sheet) {
        this.sheet = sheet;
    }

    public String getTitle() {
        return sheet.getProperties().getTitle();
    }

    // Add other necessary methods to interact with the sheet
}
