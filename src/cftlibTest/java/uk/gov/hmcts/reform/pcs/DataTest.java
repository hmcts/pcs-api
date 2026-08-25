package uk.gov.hmcts.reform.pcs;

import feign.FeignException;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.client.TestingSupportClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.model.PartyAccessCode;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.reform.pcs.service.ResponseCreationService;
import uk.gov.hmcts.reform.pcs.service.FeePaymentService;
import uk.gov.hmcts.reform.pcs.service.CaseStateService;
import uk.gov.hmcts.reform.pcs.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.testingsupport.model.PartyEmail;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private ResponseCreationService responseCreationService;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private CaseStateService caseStateService;
    @Autowired
    private FeePaymentService feePaymentService;
    @Autowired
    private AccessCodeService accessCodeService;
    @Autowired
    private TestingSupportClient testingSupportClient;

    private static final String CITIZEN_EMAIL_ADDRESS = "test@test.com";
    private String citizenToken;

    private String solicitorToken;
    private long caseReference;

    @BeforeAll
    void setUpAndPopulate() {
        try {
            solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
            citizenToken = idamClient.getAccessToken("citizen@pcs.com", "password");

            caseReference = caseCreationService.createMaximalCase(solicitorToken);

            List<FeePaymentSummary> feePaymentSummaries =
                feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);
            feePaymentService.simulatePayments(caseReference, feePaymentSummaries);

            caseStateService.waitForCaseState(caseReference, State.CASE_ISSUED, solicitorToken);

            List<PartyAccessCode> partyAccessCodes = accessCodeService.waitForAccessCodes(caseReference);
            PartyAccessCode partyAccessCode = partyAccessCodes.getFirst();
            accessCodeService.linkUserToCase(caseReference, partyAccessCode.getAccessCode(), citizenToken);

            PartyEmail partyEmail = PartyEmail.builder()
                .partyId(partyAccessCode.getPartyId())
                .emailAddress(CITIZEN_EMAIL_ADDRESS)
                .build();
            testingSupportClient.setPartyEmail(partyEmail, citizenToken);

            responseCreationService.createDefendantResponse(caseReference, citizenToken);

            log.info("CASE + RESPONSE CREATED - CASE REF: {}", caseReference);

        } catch (FeignException e) {
            log.error("ERROR CREATING CASE/RESPONSE - Status: {}, Body: {}", e.status(), e.contentUTF8());
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
        String msgValidAddress = "Property address linked to case is incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("address validations",
                                                   () -> assertHasColumns("public.address", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(0, duplicateIds, msgDupId),
                                                   () -> assertEquals(0, nullLine1, msgLine1),
                                                   () -> assertEquals(0, nullPostcode, msgPostcode),
                                                   () -> assertEquals(1, validPropertyAddresses,  msgValidAddress)
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
                                                   () -> assertEquals(1, validTenancy,  msgValidTenancy)
        );
    }

    // claim table validation
    @Test
    @DisplayName("validate public.claim - schema, flags, and relationship rules")
    void validateClaimTable() {
        List<String> expectedColumns = List.of(
            "id", "version", "case_id", "claimant_type", "against_trespassers",
            "due_to_rent_arrears", "claim_costs", "pre_action_protocol_followed",
            "mediation_attempted", "settlement_attempted", "language_used"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.claim");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.claim cl "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
        );

        int validClaim = runCountQuery(
            "SELECT COUNT(*) FROM public.claim cl "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
                + "AND cl.due_to_rent_arrears = 'YES' "
                + "AND cl.against_trespassers = 'NO' "
                + "AND cl.pre_action_protocol_followed = 'YES' "
                + "AND cl.mediation_attempted = 'YES' "
                + "AND cl.settlement_attempted = 'YES' "
                + "AND cl.language_used = 'ENGLISH' "
        );

        String msgCount = "Expected tenancy_licence to have a row, found " + totalRows;
        String msgCasePresent = "Expected created case_reference " + caseReference + " to exist";
        String msgValidClaim = "Claim detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("claim validations",
                                                   () -> assertHasColumns("public.claim", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validClaim,  msgValidClaim)
        );
    }

    // party table validation
    @Test
    @DisplayName("validate public.party - schema, names, and relationship rules")
    void validatePartyTable() {
        List<String> expectedColumns = List.of(
            "id", "version", "case_id", "type", "idam_id", "first_name",
            "last_name", "org_name", "email_address", "phone_number"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.party");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.party p "
                + "JOIN public.pcs_case c ON p.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
        );

        int validDefendant = runCountQuery(
            "SELECT COUNT(*) FROM public.party p "
                + "JOIN public.pcs_case c ON p.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND p.first_name = 'Dominic' "
                + "AND p.last_name = 'Defendant'"
                + "AND p.address_known = 'YES' "
                + "AND p.address_same_as_property = 'YES' "
                + "AND p.name_known = 'YES' "
        );

        int validClaimant = runCountQuery(
            "SELECT COUNT(*) FROM public.party p "
                + "JOIN public.pcs_case c ON p.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND p.org_name = 'TreeTops Housing'"
                + "AND p.phone_number_provided = 'YES' "
                + "AND p.phone_number = '00000000000'"
                + "AND p.name_overridden = 'NO' "
        );

        String msgCount = "Expected party to have a row, found " + totalRows;
        String msgCasePresent = "Expected party case_reference " + caseReference + " to exist and have 2 rows";
        String msgValidDefendant = "Defendant row party fields linked to case are incorrectly populated";
        String msgValidClaimant = "Claimant row party fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("party validations",
                                                   () -> assertHasColumns("public.party", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertTrue(createdCasePresent >= 2, msgCasePresent),
                                                   () -> assertEquals(1, validDefendant,  msgValidDefendant),
                                                   () -> assertEquals(1, validClaimant,  msgValidClaimant)
        );
    }

    // document table validation - add type
    @Test
    @DisplayName("validate public.document - schema, URLs, and relationship rules")
    void validateDocumentTable() {
        List<String> expectedColumns = List.of(
            "file_name", "url", "binary_url", "category_id",
            "type", "document_id", "submitted_date"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.document");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.document d "
                + "JOIN public.pcs_case c ON d.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
        );

        int validDocument = runCountQuery(
            "SELECT COUNT(*) FROM public.document d "
                + "JOIN public.pcs_case c ON d.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
                + "AND d.url = 'https://docstore/document/00000000-AA00-0000-A000-A0AA000A0000' "
                + "AND d.file_name = 'rent-statement - Claimant 1.pdf' "
                + "AND d.binary_url = 'https://docstore/document/00000000-AA00-0000-A000-A0AA000A0000/binary' "
                + "AND d.category_id = 'propertyDocuments' "
                + "AND d.type = 'RENT_STATEMENT' "
                + "AND d.document_id = '00000000-aa00-0000-a000-a0aa000a0000' "
                + "AND date_trunc('hour', d.submitted_date) = date_trunc('hour', CURRENT_TIMESTAMP)"
        );

        String msgCount = "Expected document table to have rows";
        String msgCasePresent = "Expected created case" + caseReference + "to exist";
        String msgValidDocument = "Document detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("document validations",
                                                   () -> assertHasColumns("public.document", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(3, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validDocument,  msgValidDocument)
        );
    }

    // statement of truth table validation
    @Test
    @DisplayName("validate public.statement_of_truth - schema, URLs, and relationship rules")
     void validateStatementOfTruthTable() {
        List<String> expectedColumns = List.of(
            "id", "claim_id", "completed_by", "accepted",
            "full_name", "firm_name", "position_held"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.statement_of_truth");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.statement_of_truth s "
                + "JOIN public.claim cl ON s.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
        );

        int validStatementOfTruth = runCountQuery(
            "SELECT COUNT(*) FROM public.statement_of_truth s "
                + "JOIN public.claim cl ON s.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = '" + caseReference + "'"
                + "AND s.completed_by = 'CLAIMANT' "
                + "AND s.accepted = 'YES' "
                + "AND s.full_name = 'TreeTops Housing Representative' "
                + "AND s.position_held = 'Housing Manager' "
        );

        String msgCount = "Expected statement_of_truth table to have rows";
        String msgCasePresent = "Expected statement_of_truth case_reference " + caseReference + " to exist";
        String msgValidSoT = "Statement of truth detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("statement_of_truth validations",
                                                   () -> assertHasColumns("public.statement_of_truth", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(2, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validStatementOfTruth,  msgValidSoT)
        );
    }

    // rent_arrears table validation
    @Test
    @DisplayName("validate public.rent_arrears - schema, completeness, and relationship rules")
    void validateRentArrearsTable() {
        List<String> expectedColumns = List.of(
            "id", "total_rent_arrears", "arrears_judgment_wanted",
            "recovery_attempted", "recovery_attempt_details"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.rent_arrears");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.rent_arrears ra "
                + "JOIN public.claim cl ON ra.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        int validRentArrears = runCountQuery(
            "SELECT COUNT(*) FROM public.rent_arrears ra "
                + "JOIN public.claim cl ON ra.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND ra.total_rent_arrears = 2000.00 "
                + "AND ra.arrears_judgment_wanted = 'YES' "
                + "AND ra.recovery_attempted = 'NO'"
        );

        String msgCount = "Expected rent_arrears table to have rows";
        String msgCasePresent = "Expected rent_arrears case_reference " + caseReference + " to exist";
        String msgValidArrears = "Rent arrears detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("rent_arrears validations",
                                                   () -> assertHasColumns("public.rent_arrears", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validRentArrears, msgValidArrears)
        );
    }

    // claim_ground table validation
    @Test
    @DisplayName("validate public.claim_ground - schema, completeness, and relationship rules")
    void validateClaimGroundTable() {
        List<String> expectedColumns = List.of(
            "id", "claim_id", "category", "code", "reason", "description", "is_rent_arrears"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.claim_ground");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.claim_ground cg "
                + "JOIN public.claim cl ON cg.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        int validClaimGround = runCountQuery(
            "SELECT COUNT(*) FROM public.claim_ground cg "
                + "JOIN public.claim cl ON cg.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND cg.category = 'ASSURED_MANDATORY' "
                + "AND cg.code = 'SERIOUS_RENT_ARREARS_GROUND8' "
                + "AND cg.is_rent_arrears = true"
        );

        String msgCount = "Expected claim_ground table to have rows";
        String msgCasePresent = "Expected claim_ground case_reference " + caseReference + " to exist";
        String msgValidGround = "Claim ground detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("claim_ground validations",
                                                   () -> assertHasColumns("public.claim_ground", expectedColumns),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validClaimGround, msgValidGround)
        );
    }

    // notice_of_possession table validation
    @Test
    @DisplayName("validate public.notice_of_possession - schema, completeness, and relationship rules")
    void validateNoticeOfPossessionTable() {
        List<String> expectedCols = List.of(
            "id", "claim_id", "notice_served", "notice_type", "serving_method",
            "notice_details", "notice_date", "notice_date_time", "notice_statement",
            "unable_to_upload_reason", "is_able_to_upload_document"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.notice_of_possession");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.notice_of_possession np "
                + "JOIN public.claim cl ON np.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        int validNoticeOfPossession = runCountQuery(
            "SELECT COUNT(*) FROM public.notice_of_possession np "
                + "JOIN public.claim cl ON np.claim_id = cl.id "
                + "JOIN public.pcs_case c ON cl.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND np.notice_served = 'YES'"
                + "AND np.serving_method = 'PERSONALLY_HANDED' "
                + "AND np.notice_details = 'Dominic Defendant' "
                + "AND np.is_able_to_upload_document = 'NO' "
                + "AND np.unable_to_upload_reason = 'Notice was served by hand, no digital copy available' "
        );

        String msgCount = "Expected notice_of_possession table to have rows";
        String msgCasePresent = "Expected notice_of_possession case_reference " + caseReference + " to exist";
        String msgValidNotice = "Notice of possession detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("notice_of_possession validations",
                                                   () -> assertHasColumns("public.notice_of_possession", expectedCols),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validNoticeOfPossession, msgValidNotice)
        );
    }

    // defendant_response table validation
    @Test
    @DisplayName("validate public.defendant_response - schema, completeness, and relationship rules")
    void validateDefendantResponseTable() {
        List<String> expectedCols = List.of(
            "id", "claim_id", "party_id", "pcs_case_id", "free_legal_advice",
            "tenancy_start_date_confirmation", "defendant_name_confirmation",
            "correspondence_address_confirmation", "dispute_claim", "make_counter_claim",
            "status", "language_used", "tenancy_type_confirmation", "exempt_landlord",
            "written_terms"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.defendant_response");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.defendant_response dr "
                + "JOIN public.pcs_case c ON dr.pcs_case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        int validDefendantResponse = runCountQuery(
            "SELECT COUNT(*) FROM public.defendant_response dr "
                + "JOIN public.pcs_case c ON dr.pcs_case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND dr.free_legal_advice = 'NO' "
                + "AND dr.defendant_name_confirmation = 'YES' "
                + "AND dr.dispute_claim = 'NO' "
                + "AND dr.make_counter_claim = 'NO' "
                + "AND dr.language_used = 'ENGLISH' "
        );

        String msgCount = "Expected defendant_response table to have rows";
        String msgCasePresent = "Expected defendant_response case_reference " + caseReference + " to exist";
        String msgValidResponse = "Defendant response detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("defendant_response validations",
                                                   () -> assertHasColumns("public.defendant_response", expectedCols),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validDefendantResponse, msgValidResponse)
        );
    }

    // regular_income table validation
    @Test
    @DisplayName("validate public.regular_income - schema, completeness, and relationship rules")
    void validateRegularIncomeTable() {
        List<String> expectedCols = List.of(
            "id", "hc_id", "other_income_details", "created_at", "updated_at"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.regular_income");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.regular_income ri "
                + "JOIN public.household_circumstances hc ON ri.hc_id = hc.id "
                + "JOIN public.defendant_response dr ON hc.defendant_response_id = dr.id "
                + "JOIN public.pcs_case c ON dr.pcs_case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        String msgCount = "Expected regular_income table to have rows";
        String msgCasePresent = "Expected regular_income case_reference " + caseReference + " to exist";

        org.junit.jupiter.api.Assertions.assertAll("regular_income validations",
                                                   () -> assertHasColumns("public.regular_income", expectedCols),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent)
        );
    }

    // contact_preferences table validation
    @Test
    @DisplayName("validate public.contact_preferences - schema, completeness, and relationship rules")
    void validateContactPreferencesTable() {
        List<String> expectedCols = List.of(
            "id", "contact_by_text", "contact_by_phone", "preference_type",
            "contact_by_email", "contact_by_post"
        );

        int totalRows = runCountQuery("SELECT COUNT(*) FROM public.contact_preferences");

        int createdCasePresent = runCountQuery(
            "SELECT COUNT(*) FROM public.contact_preferences cp "
                + "JOIN public.party p ON p.contact_preferences_id = cp.id "
                + "JOIN public.pcs_case c ON p.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference
        );

        int validContactPreferences = runCountQuery(
            "SELECT COUNT(*) FROM public.contact_preferences cp "
                + "JOIN public.party p ON p.contact_preferences_id = cp.id "
                + "JOIN public.pcs_case c ON p.case_id = c.id "
                + "WHERE c.case_reference = " + caseReference + " "
                + "AND cp.contact_by_text = 'YES' "
                + "AND cp.contact_by_phone = 'YES' "
                + "AND cp.contact_by_email = 'YES' "
                + "AND cp.contact_by_post = 'YES' "
                + "AND cp.preference_type IS NULL"
        );

        String msgCount = "Expected contact_preferences table to have rows";
        String msgCasePresent = "Expected contact_preferences case_reference " + caseReference + " to exist";
        String msgValidPrefs = "Contact preferences detail fields linked to case are incorrectly populated";

        org.junit.jupiter.api.Assertions.assertAll("contact_preferences validations",
                                                   () -> assertHasColumns("public.contact_preferences", expectedCols),
                                                   () -> assertTrue(totalRows > 0, msgCount),
                                                   () -> assertEquals(1, createdCasePresent, msgCasePresent),
                                                   () -> assertEquals(1, validContactPreferences, msgValidPrefs)
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
