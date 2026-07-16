/*
* Instructions to run:
* 1) Run "./gradlew cftlibtest"  to populate the cftlibtest database
* 2) Run this test using "./gradlew test --tests uk.gov.hmcts.reform.pcs.DataTest --rerun"
* 3) View test results by running "open build/reports/tests/test/index.html"
* */

package uk.gov.hmcts.reform.pcs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataTest {

    private static Connection connection;

    @BeforeAll
    static void setUpConnection() throws Exception {
        String host = System.getenv().getOrDefault("PCS_DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("PCS_DB_PORT", "6432");
        String dbName = System.getenv().getOrDefault("PCS_DB_NAME", "pcs");
        String dbOptions = System.getenv().getOrDefault("PCS_DB_OPTIONS", "");
        String user = System.getenv().getOrDefault("PCS_DB_USER_NAME", "postgres");
        String password = System.getenv().getOrDefault("PCS_DB_PASSWORD", "postgres");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + dbOptions;

        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection(url, user, password);
    }

    @AfterAll
    static void tearDownConnection() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // pcs_case Schema

    @Test
    @DisplayName("pcs_case table contains all expected columns")
    void pcsCaseHasExpectedSchema() throws Exception {
        List<String> expectedColumns = Arrays.asList(
            "id", "version", "property_address_id", "case_reference",
            "base_location", "region_id", "claimant_type", "party_documents",
            "legislative_country", "pre_action_protocol_completed", "case_management_location"
        );
        assertHasColumns("public.pcs_case", expectedColumns);
    }

    // pcs_case Data Quality

    @Test
    @DisplayName("pcs_case has at least one row - ensure journey actually populated the table)")
    void pcsCaseHasRows() throws Exception {
        int rowCount = runCountQuery("SELECT COUNT(*) FROM public.pcs_case");
        assertTrue(rowCount > 0, "Expected pcs_case to have at least one row, found " + rowCount);
    }

    @Test
    @DisplayName("pcs_case.id is unique")
    void pcsCaseIdIsUnique() throws Exception {
        int duplicateGroups = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "  SELECT id FROM public.pcs_case GROUP BY id HAVING COUNT(*) > 1"
                + ") duplicates"
        );
        assertEquals(0, duplicateGroups,
                     () -> "Found " + duplicateGroups + " duplicate 'id' value(s) in pcs_case — expected all unique");
    }

    @Test
    @DisplayName("pcs_case.case_reference is unique")
    void caseReferenceIsUnique() throws Exception {
        int duplicateGroups = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "  SELECT case_reference FROM public.pcs_case"
                + "  WHERE case_reference IS NOT NULL"
                + "  GROUP BY case_reference HAVING COUNT(*) > 1"
                + ") duplicates"
        );
        assertEquals(0, duplicateGroups,
                     () -> "Found " + duplicateGroups + " duplicate 'case_reference' value(s) — expected all unique");
    }

    @Test
    @DisplayName("pcs_case.case_reference has zero nulls")
    void caseReferenceIsComplete() throws Exception {
        int nullCount = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case WHERE case_reference IS NULL"
        );
        assertEquals(0, nullCount,
                     () -> "Found " + nullCount + " NULL value(s) in 'case_reference' — expected 0");
    }

    @Test
    @DisplayName("pcs_case.property_address_id has zero nulls")
    void propertyAddressIdIsComplete() throws Exception {
        int nullCount = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case WHERE property_address_id IS NULL"
        );
        assertEquals(0, nullCount,
                     () -> "Found " + nullCount + " NULL value(s) in 'property_address_id' — expected 0");
    }

    @Test
    @DisplayName("pcs_case.legislative_country only contains allowed values")
    void legislativeCountryIsContainedInAllowedSet() throws Exception {
        // Adjust this list if more legislative countries are valid in your domain
        int invalidCount = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case "
                + "WHERE legislative_country IS NOT NULL "
                + "AND legislative_country NOT IN ('ENGLAND', 'WALES')"
        );
        assertEquals(0, invalidCount,
                     () -> "Found " + invalidCount + " row(s) with unexpected 'legislative_country' value");
    }

    @Test
    @DisplayName("every pcs_case.property_address_id has a matching address row")
    void everyCaseHasMatchingAddress() throws Exception {
        int orphanCount = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case c "
                + "LEFT JOIN public.address a ON c.property_address_id = a.id "
                + "WHERE c.property_address_id IS NOT NULL AND a.id IS NULL"
        );
        assertEquals(0, orphanCount,
                     () -> "Found " + orphanCount
                         + " pcs_case row(s) referencing a property_address_id with no matching address row");
    }

    // address schema

    @Test
    @DisplayName("address table contains all expected columns")
    void addressHasExpectedSchema() throws Exception {
        List<String> expectedColumns = Arrays.asList(
            "id", "version", "address_line1", "address_line2", "address_line3",
            "post_town", "county", "postcode", "country"
        );
        assertHasColumns("public.address", expectedColumns);
    }

    // address data quality

    @Test
    @DisplayName("address has at least one row")
    void addressHasRows() throws Exception {
        int rowCount = runCountQuery("SELECT COUNT(*) FROM public.address");
        assertTrue(rowCount > 0, "Expected address to have at least one row, found " + rowCount);
    }

    @Test
    @DisplayName("address.id is unique")
    void addressIdIsUnique() throws Exception {
        int duplicateGroups = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "  SELECT id FROM public.address GROUP BY id HAVING COUNT(*) > 1"
                + ") duplicates"
        );
        assertEquals(0, duplicateGroups,
                     () -> "Found " + duplicateGroups + " duplicate 'id' value(s) in address — expected all unique");
    }

    @Test
    @DisplayName("address.address_line1 has correct value")
    void addressLine1CorrectValue() throws Exception {
        int valueCount = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE address_line1 != '123 Baker Street'"
        );
        assertEquals(0, valueCount, "Found incorrect value(s) in 'address_line1' — didn't find '123 Baker Street'");
    }

    @Test
    @DisplayName("address.address_line2 has correct value")
    void addressLine2CorrectValue() throws Exception {
        int valueCount = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE address_line2 != 'Marylebone'"
        );
        assertEquals(0, valueCount, "Found incorrect value(s) in 'address_line2' — didn't find 'Marylebone'");
    }

    @Test
    @DisplayName("address.post_town has correct value")
    void addressPostTownCorrectValue() throws Exception {
        int valueCount = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE post_town != 'London'"
        );
        assertEquals(0, valueCount, "Found incorrect value(s) in 'post_town' — didn't find 'London'");
    }

    @Test
    @DisplayName("address.county has correct value")
    void addressCountyCorrectValue() throws Exception {
        int valueCount = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE county != 'Greater London'"
        );
        assertEquals(0, valueCount, "Found incorrect value(s) in 'county' — didn't find 'Greater London'");
    }

    @Test
    @DisplayName("address.postcode has correct value")
    void addressPostcodeCorrectValue() throws Exception {
        int valueCount = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE postcode != 'NW1 6XE'"
        );
        assertEquals(0, valueCount, "Found incorrect value(s) in 'postcode' — didn't find 'NW1 6XE'");
    }

    // helper

    // checks that a fully-qualified table (e.g. "public.pcs_case") has every column in expectedColumns
    private void assertHasColumns(String qualifiedTable, List<String> expectedColumns) throws SQLException {
        String[] parts = qualifiedTable.split("\\.", 2);
        String schema = parts.length == 2 ? parts[0] : "public";
        String table = parts.length == 2 ? parts[1] : parts[0];

        String sql = "SELECT column_name FROM information_schema.columns "
            + "WHERE table_schema = ? AND table_name = ?";

        List<String> actualColumns = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, table);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    actualColumns.add(rs.getString("column_name"));
                }
            }
        }

        List<String> missingColumns = new ArrayList<>();
        for (String expectedColumn : expectedColumns) {
            if (!actualColumns.contains(expectedColumn)) {
                missingColumns.add(expectedColumn);
            }
        }

        assertTrue(missingColumns.isEmpty(),
                   () -> "Missing expected column(s) in " + qualifiedTable + ": " + missingColumns);
    }

    private int runCountQuery(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
