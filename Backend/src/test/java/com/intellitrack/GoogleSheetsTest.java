package com.intellitrack;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class GoogleSheetsTest {

    private static final String APPLICATION_NAME = "IntelliTrack Test";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE
    );

    @Test
    public void testCreateSpreadsheet() throws Exception {
        // Load credentials
        InputStream credentialsStream = getClass().getClassLoader().getResourceAsStream("google-service-account.json");
        if (credentialsStream == null) {
            throw new RuntimeException("Credentials file not found!");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(SCOPES);

        // Create Sheets service
        Sheets sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Try to create a spreadsheet
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties().setTitle("Test Spreadsheet"));
        Spreadsheet result = sheetsService.spreadsheets().create(spreadsheet).execute();

        System.out.println("Spreadsheet created successfully! ID: " + result.getSpreadsheetId());
        System.out.println("URL: https://docs.google.com/spreadsheets/d/" + result.getSpreadsheetId());
    }
}