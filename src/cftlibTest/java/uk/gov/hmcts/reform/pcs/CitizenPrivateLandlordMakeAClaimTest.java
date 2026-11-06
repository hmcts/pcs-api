package uk.gov.hmcts.reform.pcs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimOtherInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWhoIsClaiming;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createTestAddress;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CitizenPrivateLandlordMakeAClaimTest extends CftlibTest {

    private static final String DRAFT_PAGE_ID =
        EventId.citizenMakePossessionClaim.name() + CitizenMakePossessionClaimDraftSavePage.PAGE_ID;

    // Mapped to Bradford (88516), a whitelisted England court, by db/migration/V026__pilot_postcodes.sql
    private static final AddressUK PROPERTY_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street")
        .postTown("Bradford")
        .postCode("BD10 0JB")
        .build();

    @Autowired
    private CcdClient ccdClient;
    @Autowired
    private IdamClient idamClient;
    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;

    @BeforeAll
    void setup() {
        citizenToken = idamClient.getAccessToken("citizen@pcs.com", "password");
    }

    @Test
    void citizenCanCreateSaveAndSubmitAClaim() throws Exception {
        CitizenClaimEligibility eligibility = CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD)
            .isTrespass(VerticalYesNo.NO)
            .whoIsClaiming(CitizenClaimWhoIsClaiming.MYSELF)
            .build();
        PCSCase createData = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder()
                                        .propertyAddress(PROPERTY_ADDRESS)
                                        .eligibility(eligibility)
                                        .build())
            .build();

        CaseDetails created = ccdClient.createCase(EventId.citizenCreatePossessionClaim, createData, citizenToken);
        long caseReference = created.getId();
        assertThat(created.getState()).isEqualTo(State.AWAITING_SUBMISSION_TO_HMCTS.name());

        StartEventResponse start = ccdClient.startEvent(
            EventId.citizenMakePossessionClaim, caseReference, citizenToken);
        CitizenPossessionClaim startedDraft = toPcsCase(start.getCaseDetails().getData()).getCitizenPossessionClaim();
        assertThat(startedDraft.getEligibility().getClaimantType()).isEqualTo(ClaimantType.PRIVATE_LANDLORD);

        CitizenPossessionClaim answers = completeAnswers(eligibility);
        CaseDetails saved = ccdClient.midEvent(
            EventId.citizenMakePossessionClaim, caseReference, DRAFT_PAGE_ID,
            PCSCase.builder().citizenPossessionClaim(answers).build(), start.getToken(), citizenToken);
        CitizenPossessionClaim savedDraft = toPcsCase(saved.getData()).getCitizenPossessionClaim();
        assertThat(savedDraft.getClaimant().getFirstName()).isEqualTo("Jane");

        PCSCase reviewed = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder().draftVersion(savedDraft.getDraftVersion()).build())
            .build();
        CaseResource submitted = ccdClient.submitEvent(
            EventId.citizenMakePossessionClaim, caseReference, reviewed, start.getToken(), citizenToken);

        assertThat(submitted.getState()).isEqualTo(State.PENDING_CASE_ISSUED.name());

        CitizenClaimSubmission submission = toPcsCase(
            ccdClient.getCaseDetails(caseReference, citizenToken).getData()).getCitizenPossessionClaim()
            .getSubmission();
        assertThat(submission.getServiceRequestReference()).isNotBlank();
        assertThat(submission.getFeeAmount()).isPositive();
    }

    private PCSCase toPcsCase(Map<String, Object> data) {
        return objectMapper.convertValue(data, PCSCase.class);
    }

    private static CitizenPossessionClaim completeAnswers(CitizenClaimEligibility eligibility) {
        return CitizenPossessionClaim.builder()
            .eligibility(eligibility)
            .claimant(CitizenClaimClaimant.builder()
                          .individualOrOrganisation(CitizenClaimantKind.INDIVIDUAL)
                          .firstName("Jane")
                          .lastName("Landlord")
                          .address(createTestAddress())
                          .emailAddress("citizen@pcs.com")
                          .contactByEmail(VerticalYesNo.YES)
                          .contactByPhone(VerticalYesNo.NO)
                          .contactByText(VerticalYesNo.NO)
                          .multipleClaimants(VerticalYesNo.NO)
                          .build())
            .defendants(List.of(ListValue.<CitizenClaimDefendant>builder()
                                    .value(CitizenClaimDefendant.builder()
                                               .nameKnown(VerticalYesNo.YES)
                                               .firstName("Tom")
                                               .lastName("Tenant")
                                               .addressKnown(VerticalYesNo.YES)
                                               .addressSameAsProperty(VerticalYesNo.YES)
                                               .build())
                                    .build()))
            .additionalDefendants(VerticalYesNo.NO)
            .tenancy(CitizenClaimTenancy.builder()
                         .tenancyType(CitizenClaimTenancyType.ASSURED)
                         .tenancyStartDate(LocalDate.of(2023, 4, 1))
                         .tenancyAgreementAvailable(VerticalYesNo.NO)
                         .noTenancyAgreementReason("Lost it")
                         .build())
            .grounds(CitizenClaimGrounds.builder()
                         .hasRentArrearsGrounds(VerticalYesNo.YES)
                         .rentArrearsGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_8))
                         .hasOtherGrounds(VerticalYesNo.YES)
                         .otherGrounds(Set.of(PrivateLandlordAssuredGround.GROUND_1A))
                         .groundReasons(List.of(ListValue.<CitizenClaimGroundReason>builder()
                                                    .value(CitizenClaimGroundReason.builder()
                                                               .ground(PrivateLandlordAssuredGround.GROUND_1A)
                                                               .reason("I am selling the property.")
                                                               .build())
                                                    .build()))
                         .build())
            .notice(CitizenClaimNotice.builder().noticeServed(VerticalYesNo.NO).build())
            .rentArrears(CitizenClaimRentArrears.builder()
                             .rentAmount(new BigDecimal("1250.00"))
                             .rentFrequency(RentPaymentFrequency.MONTHLY)
                             .currentArrears(new BigDecimal("5000.00"))
                             .rentHistoryTable(List.of(ListValue.<CitizenClaimRentPeriodEntry>builder()
                                                           .value(CitizenClaimRentPeriodEntry.builder()
                                                                      .periodStart(LocalDate.of(2026, 10, 1))
                                                                      .amountDue(new BigDecimal("1250.00"))
                                                                      .build())
                                                           .build()))
                             .moneyJudgmentWanted(VerticalYesNo.YES)
                             .build())
            .otherInformation(CitizenClaimOtherInformation.builder()
                                  .languageUsed(LanguageUsed.ENGLISH)
                                  .generalApplicationIntent(VerticalYesNo.NO)
                                  .build())
            .submission(CitizenClaimSubmission.builder()
                            .statementOfTruthAgreed(VerticalYesNo.YES)
                            .statementOfTruthFullName("Jane Landlord")
                            .build())
            .build();
    }

}
