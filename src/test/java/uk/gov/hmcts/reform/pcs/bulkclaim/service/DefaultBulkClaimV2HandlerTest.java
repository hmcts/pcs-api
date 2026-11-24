package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimPaymentStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimOrganisationException;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimOrganisationException.Reason;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.MortgageBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.RentalBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimResult.Outcome;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimSubmissionService.ClaimInput;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimResponseBuilder;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBulkClaimV2HandlerTest {

    private static final String ORG = "ORG1";
    private static final String SET_PBA = "PBA1234567";

    private final BulkClaimXmlCodec codec = new BulkClaimXmlCodec();
    private final BulkClaimResponseBuilder responseBuilder = new BulkClaimResponseBuilder();
    private final BulkClaimProperties properties = new BulkClaimProperties();
    private final OrganisationDetailsResponse organisation = OrganisationDetailsResponse.builder()
        .organisationIdentifier(ORG).build();

    @Mock
    private BulkClaimantOrganisationResolver organisationResolver;
    @Mock
    private BulkClaimSubmissionService submissionService;
    @Mock
    private BulkClaimSubmissionRepository submissionRepository;
    @Mock
    private BulkClaimCaseStateReader caseStateReader;
    @Mock
    private RentalBulkClaimPreparer rentalPreparer;
    @Mock
    private MortgageBulkClaimPreparer mortgagePreparer;
    @Captor
    private ArgumentCaptor<List<ClaimInput>> inputs;

    private DefaultBulkClaimV2Handler underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefaultBulkClaimV2Handler(codec, responseBuilder, properties, organisationResolver,
                                                  submissionService, submissionRepository, caseStateReader,
                                                  rentalPreparer, mortgagePreparer);
    }

    @Test
    void shouldReturnSuccessXmlWhenEveryClaimSucceeds() throws IOException {
        givenUsableOrganisation();
        when(submissionService.submit(eq(ORG), eq(organisation), any(), eq(false)))
            .thenReturn(List.of(responseBuilder.claimSuccess("CLAIM-0001", "1781000000000001")));

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 false);

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        assertThat(body(response))
            .contains("<BulkClaimResponse")
            .containsPattern("<BulkStatus>\\s*<StatusCode>1</StatusCode>")
            .contains("<CustomerUniqueIdentifier>CLAIM-0001</CustomerUniqueIdentifier>")
            .contains("<CaseReference>1781000000000001</CaseReference>")
            .contains("<ClaimNumber>1781-0000-0000-0001</ClaimNumber>");
        verify(submissionService).submit(eq(ORG), eq(organisation), inputs.capture(), eq(false));
        assertThat(inputs.getValue()).singleElement().satisfies(input -> {
            assertThat(input.customerUniqueIdentifier()).isEqualTo("CLAIM-0001");
            assertThat(input.claimKind()).isEqualTo(BulkClaimKind.RENTAL);
            assertThat(input.claimHash()).isNotBlank();
            assertThat(input.schemaVersion()).isEqualTo("2.0");
        });
    }

    @Test
    void shouldReportClaimsFailedWhenAnyClaimFails() throws IOException {
        givenUsableOrganisation();
        when(submissionService.submit(any(), any(), any(), anyBoolean()))
            .thenReturn(List.of(responseBuilder.claimFailure("CLAIM-0001", BulkFailReason.VALIDATION_FAILED, null)));

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 false);

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        assertThat(body(response))
            .containsPattern("<BulkStatus>\\s*<StatusCode>2</StatusCode>(?s:.*?)<FailReasonCode>4</FailReasonCode>");
    }

    @Test
    void shouldPassValidateOnlyThrough() throws IOException {
        givenUsableOrganisation();
        when(submissionService.submit(any(), any(), any(), eq(true)))
            .thenReturn(List.of(responseBuilder.claimSuccess("CLAIM-0001", null)));

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 true);

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        assertThat(body(response)).doesNotContain("<CaseReference>");
    }

    @Test
    void shouldReportSchemaErrorsOnAClaimResponseWithoutIdentifier() throws IOException {
        BulkClaimResult response = underTest.submitRental(sample("rental-invalid.xml"), ORG, false);

        assertThat(response.outcome()).isEqualTo(Outcome.SCHEMA_INVALID);
        assertThat(body(response))
            .containsPattern("<BulkStatus>\\s*<StatusCode>2</StatusCode>(?s:.*?)<FailReasonCode>3</FailReasonCode>")
            .contains("<ValidationErrors>")
            .contains("<Code>SCHEMA_INVALID</Code>")
            .containsPattern("<Path>line \\d+, column \\d+</Path>")
            .doesNotContain("<CustomerUniqueIdentifier>");
        verifyNoInteractions(organisationResolver, submissionService);
    }

    @Test
    void shouldRefuseWhenTooManyClaims() throws IOException {
        properties.setMaxClaimsPerRequest(0);

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 false);

        assertThat(response.outcome()).isEqualTo(Outcome.TOO_MANY_CLAIMS);
        verifyNoInteractions(organisationResolver, submissionService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_FOUND", "NOT_ACTIVE", "PROFILE_NOT_USABLE", "NOT_A_SOLICITOR_ORGANISATION"})
    void shouldReportOrganisationNotUsable(String reason) throws IOException {
        when(organisationResolver.resolve(ORG)).thenThrow(new BulkClaimOrganisationException(Reason.valueOf(reason)));

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 false);

        assertThat(response.outcome()).isEqualTo(Outcome.ORGANISATION_NOT_USABLE);
        assertThat(body(response)).containsPattern("<FailReasonCode>2</FailReasonCode>");
        verifyNoInteractions(submissionService);
    }

    @Test
    void shouldProcessNoClaimsWhenSetLevelPbaIsNotValid() throws IOException {
        when(organisationResolver.resolve(ORG)).thenReturn(organisation);
        when(organisationResolver.isPbaAccountValid(organisation, SET_PBA)).thenReturn(false);

        BulkClaimResult response = underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG,
                                                                 false);

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        assertThat(body(response)).containsPattern("<FailReasonCode>1</FailReasonCode>");
        verifyNoInteractions(submissionService);
    }

    @Test
    void shouldPrepareWithTheAvailablePreparer() throws IOException {
        givenUsableOrganisation();
        when(submissionService.submit(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        BulkClaimPreparationResult rejected = new BulkClaimPreparationResult.Rejected(
            "CLAIM-0001", BulkFailReason.VALIDATION_FAILED, List.of());
        when(rentalPreparer.prepare(any(), any(), eq(organisation))).thenReturn(rejected);

        underTest.submitRental(sample("rental-england-assured-arrears.xml"), ORG, false);

        verify(submissionService).submit(any(), any(), inputs.capture(), anyBoolean());
        assertThat(inputs.getValue().getFirst().preparation().get()).isSameAs(rejected);
    }

    @Test
    void shouldPrepareMortgageClaimsWithTheMortgagePreparer() throws IOException {
        givenUsableOrganisation();
        when(submissionService.submit(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        BulkClaimPreparationResult rejected = new BulkClaimPreparationResult.Rejected(
            "CLAIM-0001", BulkFailReason.VALIDATION_FAILED, List.of());
        when(mortgagePreparer.prepare(any(), any(), eq(organisation))).thenReturn(rejected);

        underTest.submitMortgage(sample("mortgage-claim.xml"), ORG, false);

        verify(submissionService).submit(any(), any(), inputs.capture(), anyBoolean());
        ClaimInput input = inputs.getValue().getFirst();
        assertThat(input.claimKind()).isEqualTo(BulkClaimKind.MORTGAGE);
        assertThat(input.preparation().get()).isSameAs(rejected);
    }

    @Test
    void shouldReportClaimStatusWithCaseStateAndPayment() {
        BulkClaimSubmissionEntity submission = BulkClaimSubmissionEntity.builder()
            .customerUniqueIdentifier("CLAIM-0001")
            .status(BulkClaimSubmissionStatus.CASE_CREATED)
            .caseReference(1781000000000001L)
            .serviceRequestReference("2026-1234")
            .paymentStatus(BulkClaimPaymentStatus.FAILED)
            .paymentFailureReason("Account on hold")
            .build();
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifier(ORG, "CLAIM-0001"))
            .thenReturn(Optional.of(submission));
        when(caseStateReader.stateOf(1781000000000001L)).thenReturn(Optional.of("PENDING_CASE_ISSUED"));

        BulkClaimResult response = underTest.status(ORG, "CLAIM-0001");

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        assertThat(body(response))
            .contains("<BulkClaimStatusResponse")
            .containsPattern("<ClaimStatus>\\s*<StatusCode>1</StatusCode>")
            .contains("<CaseReference>1781000000000001</CaseReference>")
            .contains("<CaseState>PENDING_CASE_ISSUED</CaseState>")
            .contains("<ServiceRequestReference>2026-1234</ServiceRequestReference>")
            .contains("<PaymentStatus>FAILED</PaymentStatus>")
            .contains("<PaymentFailureReason>Account on hold</PaymentFailureReason>");
    }

    @Test
    void shouldReportFailedValidationStatusWithItsStoredReason() {
        BulkClaimSubmissionEntity submission = BulkClaimSubmissionEntity.builder()
            .customerUniqueIdentifier("CLAIM-0002")
            .status(BulkClaimSubmissionStatus.FAILED_VALIDATION)
            .failReasonCode(BulkFailReason.NO_COURT_FOR_POSTCODE.getFailReasonCode())
            .build();
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifier(ORG, "CLAIM-0002"))
            .thenReturn(Optional.of(submission));

        BulkClaimResult response = underTest.status(ORG, "CLAIM-0002");

        assertThat(body(response)).contains("<FailReasonCode>7</FailReasonCode>").doesNotContain("<CaseReference>");
        verifyNoInteractions(caseStateReader);
    }

    @Test
    void shouldReportPendingStatusForAClaimWithoutACaseYet() {
        BulkClaimSubmissionEntity submission = BulkClaimSubmissionEntity.builder()
            .customerUniqueIdentifier("CLAIM-0003")
            .status(BulkClaimSubmissionStatus.PENDING)
            .build();
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifier(ORG, "CLAIM-0003"))
            .thenReturn(Optional.of(submission));

        BulkClaimResult response = underTest.status(ORG, "CLAIM-0003");

        assertThat(body(response)).containsPattern("<ClaimStatus>\\s*<StatusCode>3</StatusCode>");
    }

    @Test
    void shouldReportUnknownClaimAsNotFoundStatus() {
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifier(ORG, "NOPE"))
            .thenReturn(Optional.empty());

        BulkClaimResult response = underTest.status(ORG, "NOPE");

        assertThat(response.outcome()).isEqualTo(Outcome.CLAIM_NOT_FOUND);
        assertThat(body(response))
            .contains("<BulkClaimStatusResponse")
            .contains("<CustomerUniqueIdentifier>NOPE</CustomerUniqueIdentifier>")
            .containsPattern("<StatusCode>2</StatusCode>(?s:.*?)<FailReasonCode>17</FailReasonCode>");
    }

    @Test
    void shouldListStatusesInRequestOrderWithNotFoundEntries() {
        BulkClaimSubmissionEntity found = BulkClaimSubmissionEntity.builder()
            .customerUniqueIdentifier("CLAIM-0001")
            .status(BulkClaimSubmissionStatus.CASE_CREATED)
            .caseReference(1781000000000001L)
            .paymentStatus(BulkClaimPaymentStatus.PAID)
            .build();
        BulkClaimSubmissionEntity pending = BulkClaimSubmissionEntity.builder()
            .customerUniqueIdentifier("CLAIM-0003")
            .status(BulkClaimSubmissionStatus.PENDING)
            .build();
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifierIn(eq(ORG), any()))
            .thenReturn(List.of(pending, found));
        when(caseStateReader.statesOf(any())).thenReturn(Map.of(1781000000000001L, "CASE_ISSUED"));

        BulkClaimResult response = underTest.statusBatch(
            statusRequest("CLAIM-0003", "UNKNOWN", "CLAIM-0001", "CLAIM-0003"), ORG);

        assertThat(response.outcome()).isEqualTo(Outcome.OK);
        String xml = body(response);
        assertThat(xml).contains("<BulkClaimStatusListResponse");
        assertThat(xml.split("<Claim>", -1)).hasSize(5);
        assertThat(xml).containsSubsequence(
            "<CustomerUniqueIdentifier>CLAIM-0003<", "<CustomerUniqueIdentifier>UNKNOWN<",
            "<CustomerUniqueIdentifier>CLAIM-0001<", "<CustomerUniqueIdentifier>CLAIM-0003<");
        assertThat(xml).containsPattern(
            "<CustomerUniqueIdentifier>UNKNOWN</CustomerUniqueIdentifier>\\s*<ClaimStatus>"
                + "\\s*<StatusCode>2</StatusCode>(?s:.*?)<FailReasonCode>17</FailReasonCode>")
            .contains("<CaseState>CASE_ISSUED</CaseState>")
            .contains("<PaymentStatus>PAID</PaymentStatus>");
        verify(submissionRepository).findByOrganisationIdAndCustomerUniqueIdentifierIn(
            eq(ORG), eq(Set.of("CLAIM-0003", "UNKNOWN", "CLAIM-0001")));
        verify(caseStateReader).statesOf(Set.of(1781000000000001L));
    }

    @Test
    void shouldReportAnotherOrganisationsIdentifierAsNotFound() {
        when(submissionRepository.findByOrganisationIdAndCustomerUniqueIdentifierIn(eq(ORG), any()))
            .thenReturn(List.of());
        when(caseStateReader.statesOf(any())).thenReturn(Map.of());

        String xml = body(underTest.statusBatch(statusRequest("THEIRS-1"), ORG));

        assertThat(xml).contains("<FailReasonCode>17</FailReasonCode>").doesNotContain("<CaseReference>");
        verify(submissionRepository).findByOrganisationIdAndCustomerUniqueIdentifierIn(eq(ORG), any());
    }

    @Test
    void shouldRefuseStatusRequestOverTheConfiguredMaximum() {
        properties.setMaxClaimsPerRequest(2);

        BulkClaimResult response = underTest.statusBatch(statusRequest("A-1", "A-2", "A-3"), ORG);

        assertThat(response.outcome()).isEqualTo(Outcome.TOO_MANY_CLAIMS);
        verifyNoInteractions(submissionRepository, caseStateReader);
    }

    @Test
    void shouldReturnSchemaInvalidForMalformedStatusRequest() {
        BulkClaimResult response = underTest.statusBatch(
            "<BulkClaimStatusRequest xmlns=\"http://hmcts.gov.uk/pcs/bulkclaim/v2\"/>"
                .getBytes(StandardCharsets.UTF_8), ORG);

        assertThat(response.outcome()).isEqualTo(Outcome.SCHEMA_INVALID);
        assertThat(body(response)).contains("<FailReasonCode>3</FailReasonCode>")
            .contains("<Code>SCHEMA_INVALID</Code>");
        verifyNoInteractions(submissionRepository, caseStateReader);
    }

    private static byte[] statusRequest(String... identifiers) {
        StringBuilder xml = new StringBuilder("<BulkClaimStatusRequest xmlns=\"" + BulkClaimXmlCodec.NAMESPACE + "\">");
        for (String identifier : identifiers) {
            xml.append("<CustomerUniqueIdentifier>").append(identifier).append("</CustomerUniqueIdentifier>");
        }
        return xml.append("</BulkClaimStatusRequest>").toString().getBytes(StandardCharsets.UTF_8);
    }

    private void givenUsableOrganisation() {
        when(organisationResolver.resolve(ORG)).thenReturn(organisation);
        when(organisationResolver.isPbaAccountValid(organisation, SET_PBA)).thenReturn(true);
    }

    private static String body(BulkClaimResult response) {
        return new String(response.xml(), StandardCharsets.UTF_8);
    }

    private byte[] sample(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/bulkclaim/v2/" + name)) {
            assertThat(stream).isNotNull();
            return stream.readAllBytes();
        }
    }
}
