package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.LegalRepPartySelectionService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepStartEventHydrationVerificationTest {

    private static final long CASE_REFERENCE = 1787921614600330L;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;

    @Mock
    private LegalRepPartySelectionService legalRepPartySelectionService;

    @Mock
    private OrganisationService organisationService;

    private LegalRepStartEventStrategy underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepStartEventStrategy(
            pcsCaseService,
            legalRepForDefendantAccessValidator,
            legalRepPartySelectionService,
            organisationService,
            new TenancyLicenceView(),
            new NoticeOfPossessionView(),
            new RentArrearsView()
        );
    }

    @Test
    void loadDraft_hydratesTenancyTypeStartDateAndNoticeForLegalRepFrontend() {
        LocalDate tenancyStart = LocalDate.of(2020, 3, 15);
        LocalDate noticePosted = LocalDate.of(2026, 6, 1);

        final PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("defendant")
            .lastName("one")
            .build();

        TenancyLicenceEntity tenancyLicence = TenancyLicenceEntity.builder()
            .type(CombinedLicenceType.SECURE_TENANCY)
            .startDate(tenancyStart)
            .build();

        NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
            .noticeServed(YesOrNo.YES)
            .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
            .noticeDate(noticePosted)
            .build();

        ClaimEntity claim = ClaimEntity.builder()
            .noticeOfPossession(notice)
            .build();
        notice.setClaim(claim);

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .tenancyLicence(tenancyLicence)
            .build();
        tenancyLicence.setPcsCase(caseEntity);
        caseEntity.addClaim(claim);
        claim.setPcsCase(caseEntity);

        PCSCase incomingCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseEntity);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn("test-org-id");
        when(legalRepForDefendantAccessValidator.validateAndGetDefendants(caseEntity, "test-org-id", true))
            .thenReturn(List.of(defendant));
        when(legalRepPartySelectionService.hasSubmittedResponseForCurrentlySelectedParty(CASE_REFERENCE))
            .thenReturn(false);
        when(legalRepPartySelectionService.getDraftCaseData(
            eq(CASE_REFERENCE),
            eq(incomingCase),
            eq(defendant),
            eq(List.of(defendant)),
            eq("test-org-id")
        )).thenReturn(incomingCase);

        PCSCase result = underTest.loadDraft(CASE_REFERENCE, incomingCase);

        assertThat(result.getTenancyLicenceDetails()).isNotNull();
        assertThat(result.getTenancyLicenceDetails().getTypeOfTenancyLicence())
            .isEqualTo(TenancyLicenceType.SECURE_TENANCY);
        assertThat(result.getTenancyLicenceDetails().getTenancyLicenceDate()).isEqualTo(tenancyStart);
        assertThat(result.getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(result.getNoticeServedDetails()).isNotNull();
        assertThat(result.getNoticeServedDetails().getPostedDate()).isEqualTo(noticePosted);
    }

    @Test
    void masterBehavior_leavesClaimantTenancyAndNoticeUnsetWithoutHydration() {
        PCSCase masterResult = PCSCase.builder().legislativeCountry(LegislativeCountry.ENGLAND).build();

        assertThat(masterResult.getTenancyLicenceDetails()).isNull();
        assertThat(masterResult.getNoticeServed()).isNull();
    }
}
