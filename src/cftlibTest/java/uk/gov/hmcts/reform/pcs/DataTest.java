package uk.gov.hmcts.reform.pcs;

/*
 * Instructions to run:
 * Run this test using "./gradlew cftlibTest --tests uk.gov.hmcts.reform.pcs.DataTest"
 * (this test now populates AND checks the data in one run — no separate
 * CreatePossessionClaimTest run needed first)
 *
 * View test results by running "open build/reports/tests/cftlibTest/index.html"
 * */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataTest extends CftlibTest {

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private CaseCreationService caseCreationService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private String solicitorToken;
    private long caseReference;

    @BeforeEach
    void setUpAndPopulate() {
        WireMock wireMock8083 = new WireMock("localhost", 8083);
        wireMock8083.register(
            any(urlMatching("/documents.*"))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                    {
                                      "status": "SUCCESS",
                                      "_embedded": {
                                        "documents": [
                                          {
                                            "_links": {
                                              "self": {
                                                "href": "https://docstore/documents/00000000-AA00-0000-A000-A0AA000A0000"
                                              },
                                              "binary": {
                                                "href": "https://docstore/documents/00000000-AA00-0000-A000-A0AA000A0000/binary"
                                              }
                                            },
                                            "originalDocumentSerialization": "rent-statement.pdf"
                                          }
                                        ]
                                      }
                                    }
                                    """))
        );

        try {
            solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
            caseReference = caseCreationService.createMaximalCase(solicitorToken);
            System.out.println("==============================================");
            System.out.println("DATA TEST CREATED CASE REF: " + caseReference);
            System.out.println("==============================================");
        } catch (Exception e) {
            System.err.println("FAILED TO CREATE CASE IN DATA TEST SETUP: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // pcs_case table validation

    @Test
    @DisplayName("validate public.pcs_case - schema, completeness, and data quality rules")
    void validatePcsCaseTable() {
        List<String> expectedColumns = List.of(
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
            "SELECT COUNT(*) FROM public.pcs_case WHERE case_reference IS NULL");
        int nullAddressIds = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case WHERE property_address_id IS NULL");

        int invalidCountries = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case "
                + "WHERE legislative_country IS NOT NULL "
                + "AND legislative_country NOT IN ('ENGLAND', 'WALES')"
        );

        int orphanAddresses = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case c "
                + "LEFT JOIN public.address a ON c.property_address_id = a.id "
                + "WHERE c.property_address_id IS NOT NULL "
                + "AND a.id IS NULL"
        );

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.pcs_case WHERE case_reference = '" + caseReference + "'"
        );

        String msgCount = "Expected pcs_case to have at >1 row, found " + totalRows;
        String msgDupId = "Found duplicate 'id' values in pcs_case";
        String msgDupRef = "Found duplicate 'case_reference' values";
        String msgNullRef = "Found NULL values in 'case_reference' — expected 0";
        String msgNullAddr = "Found NULL values in 'property_address_id' — expected 0";
        String msgCountry = "Found rows with unexpected 'legislative_country' value";
        String msgOrphan = "Found pcs_case rows referencing a non-existent address row";
        String msgCasePresent = "Expected created case_reference " + caseReference + " to exist";

        org.junit.jupiter.api.Assertions.assertAll("pcs_case validations",
                                                   () -> assertHasColumns("public.pcs_case", expectedColumns),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(0, duplicateIds, msgDupId),
                                                   () -> assertEquals(0, duplicateCaseRefs, msgDupRef),
                                                   () -> assertEquals(0, nullCaseRefs, msgNullRef),
                                                   () -> assertEquals(0, nullAddressIds, msgNullAddr),
                                                   () -> assertEquals(0, invalidCountries, msgCountry),
                                                   () -> assertEquals(0, orphanAddresses, msgOrphan)
        );
    }

    // address table validation

    @Test
    @DisplayName("validate public.address - schema, completeness and data value rules")
    void validateAddressTable() {
        List<String> expectedColumns = List.of(
            "id", "version", "address_line1", "address_line2", "address_line3",
            "post_town", "county", "postcode", "country"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.address");

        int duplicateIds = runCountQuery(
            "SELECT COUNT(*) FROM ("
                + "SELECT id FROM public.address "
                + "GROUP BY id HAVING COUNT(*) > 1) d"
        );

        int nullLine1 = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE address_line1 IS NULL");
        int nullPostcode = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE postcode IS NULL");

        int validPropertyAddresses = runCountQuery(
            "SELECT COUNT(*) FROM public.address a "
                + "JOIN public.pcs_case c ON c.property_address_id = a.id "
                + "WHERE c.case_reference = '" + caseReference + "' "
                + "AND a.address_line1 = '2 Second Avenue' "
                + "AND a.postcode = 'W3 7RX'"
        );

        String msgCount = "Expected address to have a row, found " + totalRows;
        String msgDupId = "Found duplicate 'id' values in address";
        String msgLine1 = "Found NULL values in 'address_line1' — expected 0";
        String msgPostcode = "Found NULL values in 'postcode' — expected 0";
        String msgAddress = "Property address linked to case is incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("address validations",
                                                   () -> assertHasColumns("public.address", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(0, duplicateIds, msgDupId),
                                                   () -> assertEquals(0, nullLine1, msgLine1),
                                                   () -> assertEquals(0, nullPostcode, msgPostcode),
                                                   () -> assertTrue(validPropertyAddresses > 0,  msgAddress)
        );
    }

    // tenancy_licence table validation

    @Test
    @DisplayName("validate public.tenancy_licence - schema, completeness, and relationship rules")
    void validateTenancyLicenceTable() {
        List<String> expectedColumns = List.of(
            "id", "version", "case_id", "type", "other_type_details",
            "start_date", "rent_amount", "rent_frequency", "has_copy_of_tenancy_licence"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.tenancy_licence");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.tenancy_licence tl "
                + "JOIN public.pcs_case c ON tl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
        );

        int validTenancy = runCountQuery(
            "SELECT COUNT(*) FROM public.tenancy_licence tl "
                + "JOIN public.pcs_case c ON tl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
                + "AND tl.rent_amount = '2000.00' "
                + "AND tl.rent_frequency = 'MONTHLY' "
                + "AND tl.start_date = '2025-01-01' "
                + "AND tl.has_copy_of_tenancy_licence = 'NO' "
                + "AND tl.reasons_for_no_tenancy_licence = 'Copy of agreement was lost' "
        );

        String msgCount = "Expected tenancy_licence to have a row, found " + totalRows;
        String msgCasePresent = "Expected created case_reference " + caseReference + " to exist";
        String msgValidTenancy = "Tenancy detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("tenancy_licence validations",
                                                   () -> assertHasColumns("public.tenancy_licence", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertTrue(validTenancy > 0,  msgValidTenancy)
        );
    }

    // helper

    private void assertHasColumns(String qualifiedTable, List<String> expectedColumns) {
        String[] parts = qualifiedTable.split("\\.", 2);
        String schema = parts.length == 2 ? parts[0] : "public";
        String table = parts.length == 2 ? parts[1] : parts[0];

        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("schema", schema)
            .addValue("table", table);

        List<String> actualColumns = jdbcTemplate.query(
            "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = :schema AND table_name = :table",
            params,
            (rs, rowNum) -> rs.getString("column_name")
        );

        List<String> missingColumns = expectedColumns.stream()
            .filter(col -> !actualColumns.contains(col))
            .toList();

        assertTrue(missingColumns.isEmpty(),
                   () -> "Missing expected column(s) in " + qualifiedTable + ": " + missingColumns);
    }

    private int runCountQuery(String sql) {
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
    }
}
