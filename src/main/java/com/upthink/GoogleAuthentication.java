package com.upthink;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.gmail.GmailScopes;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



public class GoogleAuthentication {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS,
            GmailScopes.GMAIL_LABELS,
            GmailScopes.GMAIL_METADATA,
            GmailScopes.GMAIL_MODIFY,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_SETTINGS_BASIC,
            GmailScopes.GMAIL_SETTINGS_SHARING);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public static Sheets build() throws GeneralSecurityException, IOException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service =
                new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, GoogleAuthentication.getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        return service;
    }


    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    public static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleAuthentication.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        Credential credential = flow.loadCredential("user-id");

        if (credential == null) {
            // No credentials found, initiate authentication
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8088).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user-id");
        } else if (credential.getExpiresInSeconds() <= 60) {
            // Token is expired or about to expire, refresh it
            credential.refreshToken();
        }

        return credential;
    }

//    public static void main(String[] args) {
//        try {
//            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
//            Credential credential = getCredentials();
//            // Use credential for authenticated requests
//        } catch (IOException | GeneralSecurityException e) {
//            e.printStackTrace();
//        }
//    }
}



//public class GoogleSheetsService() {
//
//    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static final String CREDENTIALS_PATH = "/credentials.json";
//    private static final String TOKENS_DIRECTORY_PATH = "tokens";
//
//    public static Credential authorize(final NetHttpTransport HTTPTransport) throws IOException {
//        Credential token = GoogleSheetsService.loadStoredCredentials(HTTPTransport);
//        if (token != null && (token.getExpirationTimeMilliseconds() == null || token.getExpiresInSeconds() > 60)) {
//            return token;
//        }
//        return GoogleSheetsService.authorizeUsingClientSecrets(HTTPTransport);
//    }
//
//    private static Credential loadStoredCredentials(final NetHttpTransport HTTPTransport) throws IOException {
//        File tokenFile = new File(TOKENS_DIRECTORY_PATH);
//        // If token file exists load the credential from the token file
//        if (tokenFile.exists()) {
//            FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(tokenFile);
//            dataStoreFactory.getDataStore()
//        }
//    }
//
//    private static GoogleClientSecrets loadClientSecrets() throws IOException {
//        InputStream in = GoogleSheetsService.class.getResourceAsStream(CREDENTIALS_PATH);
//        if (in == null) {
//            throw new FileNotFoundException("Resources not present: " + CREDENTIALS_PATH);
//        }
//        return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//    }
//
//    private static Credential authorizeUsingClientSecrets(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
//        GoogleClientSecrets clientSecrets = loadClientSecrets();
//
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setAccessType("offline")
//                .build();
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(0).build();
//        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//    }
//}