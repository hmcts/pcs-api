package uk.gov.hmcts.reform.pcs;

/*
 * Instructions to run:
 * Run this test using "./gradlew cftlibTest --tests uk.gov.hmcts.reform.pcs.DataTest"
 * (this test now populates AND checks the data in one run — no separate
 * CreatePossessionClaimTest run needed first)
 *
 * View test results by running "open build/reports/tests/cftlibTest/index.html"
 * */

import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    void setUpAndPopulate() {
        // populate cftlib database

        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);
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

        String msgCount = "Expected pcs_case to have at >1 row, found " + totalRows;
        String msgDupId = "Found duplicate 'id' values in pcs_case";
        String msgDupRef = "Found duplicate 'case_reference' values";
        String msgNullRef = "Found NULL values in 'case_reference' — expected 0";
        String msgNullAddr = "Found NULL values in 'property_address_id' — expected 0";
        String msgCountry = "Found rows with unexpected 'legislative_country' value";
        String msgOrphan = "Found pcs_case rows referencing a non-existent address row";

        org.junit.jupiter.api.Assertions.assertAll("pcs_case validations",
                                                   () -> assertHasColumns("public.pcs_case", expectedColumns),
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

        int correctPostTown = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE post_town != 'London'");

        int correctCounty = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE county != 'Greater London'");

        int correctPostcode = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE postcode != 'NW1 6XE'");

        int correctAddress1 = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE address_line1 != '123 Baker Street'");

        int correctAddress2 = runCountQuery(
            "SELECT COUNT(*) FROM public.address WHERE address_line2 != 'Marylebone'");

        String msgCount = "Expected address to have a row, found " + totalRows;
        String msgDupId = "Found duplicate 'id' values in address";
        String msgLine1 = "Found NULL values in 'address_line1' — expected 0";
        String msgPostcode = "Found NULL values in 'postcode' — expected 0";
        String msgPostTown = "Incorrect 'post_town' value";
        String msgCounty = "Incorrect 'county' value";
        String msgPostcodeValue = "Incorrect 'postcode' value";
        String msgAddress1 = "Incorrect 'address_line1' value";
        String msgAddress2 = "Incorrect 'address_line2' value";

        org.junit.jupiter.api.Assertions.assertAll("address validations",
                                                   () -> assertHasColumns("public.address", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(0, duplicateIds, msgDupId),
                                                   () -> assertEquals(0, nullLine1, msgLine1),
                                                   () -> assertEquals(0, nullPostcode, msgPostcode),
                                                   () -> assertEquals(0, correctPostTown, msgPostTown),
                                                   () -> assertEquals(0, correctCounty, msgCounty),
                                                   () -> assertEquals(0, correctPostcode, msgPostcodeValue),
                                                   () -> assertEquals(0, correctAddress1, msgAddress1),
                                                   () -> assertEquals(0, correctAddress2, msgAddress2)
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
