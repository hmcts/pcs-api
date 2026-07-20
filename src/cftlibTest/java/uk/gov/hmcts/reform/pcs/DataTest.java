package uk.gov.hmcts.reform.pcs;

/*
 * Instructions to run:
 * Run this test using "./gradlew cftlibTest --tests uk.gov.hmcts.reform.pcs.DataTest"
 * (this test now populates AND checks the data in one run — no separate
 * CreatePossessionClaimTest run needed first)
 *
 * View test results by running "open build/reports/tests/cftlibTest/index.html"
 * */

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamClient idamClient;

    private static Connection connection;

    @BeforeAll
    void setUpAndPopulate() throws Exception {

        // ---------- 1. Populate the database via the real CCD journey ----------

        String solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");

        PCSCase createData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .addressLine2("Marylebone")
                                 .postTown("London")
                                 .county("Greater London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(createData, solicitorToken);
        Long caseReference = caseDetails.getId();

        PCSCase resumeData = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, resumeData, solicitorToken);

        // ---------- 2. Open the JDBC connection used by the checks below ----------

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
    void tearDownConnection() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // pcs_case table validation

    @Test
    @DisplayName("validate public.pcs_case - schema, completeness, and data quality rules")
    void validatePcsCaseTable() throws Exception {
        // 1. Fetch data required for multi-row validations upfront
        List<String> expectedColumns = Arrays.asList(
            "id", "version", "property_address_id", "case_reference",
            "base_location", "region_id", "claimant_type", "party_documents",
            "legislative_country", "pre_action_protocol_completed", "case_management_location"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.pcs_case");

        int duplicateIds = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "SELECT id FROM public.pcs_case "
                + "GROUP BY id HAVING COUNT(*) > 1) d"
        );

        int duplicateCaseRefs = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "SELECT case_reference FROM public.pcs_case "
                + "WHERE case_reference IS NOT NULL "
                + "GROUP BY case_reference HAVING COUNT(*) > 1) d"
        );

        int nullCaseRefs = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM public.pcs_case "
                + "WHERE case_reference IS NULL");
        int nullAddressIds = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM public.pcs_case "
                + "WHERE property_address_id IS NULL");

        int invalidCountries = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM public.pcs_case "
                + "WHERE legislative_country IS NOT NULL "
                + "AND legislative_country NOT IN ('ENGLAND', 'WALES')"
        );

        int orphanAddresses = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM public.pcs_case "
                + "c LEFT JOIN public.address a ON c.property_address_id = a.id "
                + "WHERE c.property_address_id IS NOT NULL "
                + "AND a.id IS NULL"
        );

        // 2. Execute all assertions collectively using assertAll
        org.junit.jupiter.api.Assertions.assertAll("pcs_case validations",
           () -> assertHasColumns("public.pcs_case", expectedColumns),
           () -> assertTrue(totalRows > 0, "Expected pcs_case to have at >1 row, found " + totalRows),
           () -> assertEquals(0, duplicateIds, "Found duplicate 'id' values in pcs_case"),
           () -> assertEquals(0, duplicateCaseRefs, "Found duplicate 'case_reference' values"),
           () -> assertEquals(0, nullCaseRefs, "Found NULL values in 'case_reference' — expected 0"),
           () -> assertEquals(0, nullAddressIds, "Found NULL values in 'property_address_id' — expected 0"),
           () -> assertEquals(0, invalidCountries, "Found rows with unexpected 'legislative_country' value"),
           () -> assertEquals(0, orphanAddresses, "Found pcs_case rows referencing a non-existent address row")
        );
    }

    // address table validation

    @Test
    @DisplayName("validate public.address - schema, completeness, and value matching rules")
    void validateAddressTable() throws Exception {
        // 1. Fetch data required for column value checks upfront
        List<String> expectedColumns = Arrays.asList(
            "id", "version", "address_line1", "address_line2", "address_line3",
            "post_town", "county", "postcode", "country"
        );

        int totalRows = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM public.address");

        int duplicateIds = runCountQuery(
            "SELECT COUNT(*) "
                + "FROM ("
                + "SELECT id "
                + "FROM public.address "
                + "GROUP BY id HAVING COUNT(*) > 1) d"
        );

        int badLine1 = runCountQuery("SELECT COUNT(*) FROM public.address WHERE address_line1 != '123 Baker Street'");
        int badLine2 = runCountQuery("SELECT COUNT(*) FROM public.address WHERE address_line2 != 'Marylebone'");
        int badPostTown = runCountQuery("SELECT COUNT(*) FROM public.address WHERE post_town != 'London'");
        int badCounty = runCountQuery("SELECT COUNT(*) FROM public.address WHERE county != 'Greater London'");
        int badPostcode = runCountQuery("SELECT COUNT(*) FROM public.address WHERE postcode != 'NW1 6XE'");

        // 2. Execute all assertions collectively using assertAll
        org.junit.jupiter.api.Assertions.assertAll("address validations",
           () -> assertHasColumns("public.address", expectedColumns),
           () -> assertTrue(totalRows > 0, "Expected address to have >1, found " + totalRows),
           () -> assertEquals(0, duplicateIds, "Found duplicate 'id' values in address"),
           () -> assertEquals(0, badLine1, "Found rows with unexpected 'address_line1' value"),
           () -> assertEquals(0, badLine2, "Found rows with unexpected 'address_line2' value"),
           () -> assertEquals(0, badPostTown, "Found rows with unexpected 'post_town' value"),
           () -> assertEquals(0, badCounty, "Found rows with unexpected 'county' value"),
           () -> assertEquals(0, badPostcode, "Found rows with unexpected 'postcode' value")
        );
    }

    // helper

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
