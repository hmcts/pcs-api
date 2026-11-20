package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenMakePossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.LEGISLATIVE_COUNTRY_REQUIRED;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.POSTCODE_NOT_ASSIGNED_TO_COURT;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.PROPERTY_ADDRESS_REQUIRED;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.PROPERTY_NOT_ELIGIBLE;
import static uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND;

@ExtendWith(MockitoExtension.class)
class CitizenPossessionClaimCaseServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String POSTCODE = "SW1A 1AA";
    private static final AddressUK ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street").postTown("London").postCode(POSTCODE).build();
    private static final String USER_ID = "a7f3c1e2-0000-4000-8000-000000000001";

    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private SecurityContextService securityContextService;
    @Captor
    private ArgumentCaptor<PcsCaseEntity> caseCaptor;
    @Captor
    private ArgumentCaptor<PCSCase> draftCaptor;

    private CitizenPossessionClaimCaseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CitizenPossessionClaimCaseService(eligibilityService, pcsCaseRepository, pcsCaseService,
                                                          addressMapper, draftCaseDataService,
                                                          securityContextService);
    }

    @Test
    void shouldAcceptAnEligibleEnglishProperty() {
        eligibility(null, EligibilityStatus.ELIGIBLE, LegislativeCountry.ENGLAND);

        assertThat(underTest.checkPropertyInEngland(ADDRESS, null)).isEmpty();
    }

    @Test
    void shouldRefuseAWelshProperty() {
        eligibility(null, EligibilityStatus.ELIGIBLE, LegislativeCountry.WALES);

        assertThat(underTest.checkPropertyInEngland(ADDRESS, null)).contains(PROPERTY_NOT_IN_ENGLAND);
    }

    @Test
    void shouldRefuseAnEnglishPropertyWhoseCourtIsNotLive() {
        eligibility(null, EligibilityStatus.NOT_ELIGIBLE, LegislativeCountry.ENGLAND);

        assertThat(underTest.checkPropertyInEngland(ADDRESS, null)).contains(PROPERTY_NOT_ELIGIBLE);
    }

    @Test
    void shouldRefuseAPostcodeWithNoCourt() {
        eligibility(null, EligibilityStatus.NO_MATCH_FOUND, null);

        assertThat(underTest.checkPropertyInEngland(ADDRESS, null)).contains(POSTCODE_NOT_ASSIGNED_TO_COURT);
    }

    @Test
    void shouldRefuseAMissingAddress() {
        assertThat(underTest.checkPropertyInEngland(null, null)).contains(PROPERTY_ADDRESS_REQUIRED);
        assertThat(underTest.checkPropertyInEngland(AddressUK.builder().addressLine1("x").build(), null))
            .contains(PROPERTY_ADDRESS_REQUIRED);
    }

    @Test
    void shouldAskForTheCountryOfACrossBorderPostcode() {
        crossBorder();

        assertThat(underTest.checkPropertyInEngland(ADDRESS, null)).contains(LEGISLATIVE_COUNTRY_REQUIRED);
    }

    @Test
    void shouldRefuseACrossBorderPropertyChosenAsWales() {
        crossBorder();

        assertThat(underTest.checkPropertyInEngland(ADDRESS, LegislativeCountry.WALES))
            .contains(PROPERTY_NOT_IN_ENGLAND);
    }

    @Test
    void shouldCheckACrossBorderPropertyChosenAsEnglandAgainstTheEnglishCourt() {
        crossBorder();
        eligibility(LegislativeCountry.ENGLAND, EligibilityStatus.ELIGIBLE, LegislativeCountry.ENGLAND);

        assertThat(underTest.checkPropertyInEngland(ADDRESS, LegislativeCountry.ENGLAND)).isEmpty();
    }

    @Test
    void shouldCreateACaseOwnedByTheCitizenWithNoOrganisation() {
        AddressEntity addressEntity = new AddressEntity();
        when(addressMapper.toAddressEntityAndNormalise(ADDRESS)).thenReturn(addressEntity);
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        CitizenClaimEligibility eligibility = CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build();

        underTest.createCase(CASE_REFERENCE, ADDRESS, eligibility);

        verify(pcsCaseRepository).save(caseCaptor.capture());
        PcsCaseEntity created = caseCaptor.getValue();
        assertThat(created.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(created.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(created.getPropertyAddress()).isSameAs(addressEntity);
        assertThat(created.getParties()).singleElement().satisfies(party -> {
            assertThat(party.isClaimCreator()).isTrue();
            assertThat(party.getIdamId()).isEqualTo(USER_ID);
            assertThat(party.getOrganisationId()).isNull();
        });

        verify(draftCaseDataService).patchUnsubmittedEventData(eq(CASE_REFERENCE), draftCaptor.capture(),
                                                               eq(citizenMakePossessionClaim));
        assertThat(draftCaptor.getValue().getCitizenPossessionClaim().getEligibility()).isEqualTo(eligibility);
        assertThat(draftCaptor.getValue().getCitizenPossessionClaim().getPropertyAddress()).isEqualTo(ADDRESS);
    }

    @Test
    void shouldRecogniseOnlyTheCitizenWhoCreatedTheCase() {
        PartyEntity creator = new PartyEntity();
        creator.setClaimCreator(true);
        creator.setIdamId(USER_ID);
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.addParty(creator);

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        assertThat(underTest.isCurrentUserClaimCreator(caseEntity)).isTrue();

        when(securityContextService.getCurrentUserId()).thenReturn("someone-else");
        assertThat(underTest.isCurrentUserClaimCreator(caseEntity)).isFalse();
        verifyNoInteractions(pcsCaseService);
    }

    @Test
    void shouldInitialiseAnEmptyDraftWhenThereIsNoneAndGiveItTheCaseAddress() {
        PcsCaseEntity caseEntity = caseWithAddress();
        CitizenPossessionClaim created = CitizenPossessionClaim.builder().draftVersion(0L).build();
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, citizenMakePossessionClaim))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(PCSCase.builder().citizenPossessionClaim(created).build()));

        CitizenPossessionClaim claim = underTest.loadOrInitialiseDraft(caseEntity);

        assertThat(claim.getDraftVersion()).isZero();
        assertThat(claim.getPropertyAddress()).isEqualTo(ADDRESS);
        verify(draftCaseDataService).patchUnsubmittedEventData(eq(CASE_REFERENCE), any(PCSCase.class),
                                                               eq(citizenMakePossessionClaim));
    }

    @Test
    void shouldGiveASavedDraftTheCaseAddressNotTheOneItWasSavedWith() {
        PcsCaseEntity caseEntity = caseWithAddress();
        CitizenPossessionClaim saved = CitizenPossessionClaim.builder()
            .propertyAddress(AddressUK.builder().addressLine1("9 Moved Street").build())
            .build();
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, citizenMakePossessionClaim))
            .thenReturn(Optional.of(PCSCase.builder().citizenPossessionClaim(saved).build()));

        assertThat(underTest.loadOrInitialiseDraft(caseEntity).getPropertyAddress()).isEqualTo(ADDRESS);
    }

    @Test
    void shouldReplaceAnExistingDraftCheckingTheVersionAndDroppingServerOwnedFields() {
        CitizenPossessionClaim claim = CitizenPossessionClaim.builder()
            .propertyAddress(ADDRESS)
            .draftVersion(3L)
            .submission(CitizenClaimSubmission.builder()
                            .statementOfTruthAgreed(VerticalYesNo.YES)
                            .serviceRequestReference("forged")
                            .feeAmount(BigDecimal.TEN)
                            .paymentStatus("PAID")
                            .build())
            .build();
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, citizenMakePossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.saveUnsubmittedEventData(eq(CASE_REFERENCE), any(PCSCase.class),
                                                           eq(citizenMakePossessionClaim), eq(3L)))
            .thenReturn(4L);

        Long savedVersion = underTest.saveDraft(CASE_REFERENCE, claim);

        assertThat(savedVersion).isEqualTo(4L);
        verify(draftCaseDataService).saveUnsubmittedEventData(eq(CASE_REFERENCE), draftCaptor.capture(),
                                                              eq(citizenMakePossessionClaim), eq(3L));
        CitizenPossessionClaim stored = draftCaptor.getValue().getCitizenPossessionClaim();
        assertThat(stored.getPropertyAddress()).isNull();
        assertThat(stored.getDraftVersion()).isNull();
        assertThat(stored.getSubmission().getStatementOfTruthAgreed()).isEqualTo(VerticalYesNo.YES);
        assertThat(stored.getSubmission().getServiceRequestReference()).isNull();
        assertThat(stored.getSubmission().getFeeAmount()).isNull();
        assertThat(stored.getSubmission().getPaymentStatus()).isNull();
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(any(Long.class), any(), any());
    }

    @Test
    void shouldKeepTheConfirmedSectionsTheFrontendSent() {
        CitizenPossessionClaim claim = CitizenPossessionClaim.builder()
            .draftVersion(3L)
            .confirmedSections(Set.of(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY))
            .build();

        assertThat(CitizenPossessionClaimCaseService.withoutServerOwnedFields(claim).getConfirmedSections())
            .containsExactlyInAnyOrder(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY);
    }

    @Test
    void shouldSaveAnEmptiedConfirmedSectionsAsEmptyRatherThanLeavingItUnchanged() {
        CitizenPossessionClaim claim = CitizenPossessionClaim.builder()
            .draftVersion(3L)
            .confirmedSections(Set.of())
            .build();
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, citizenMakePossessionClaim))
            .thenReturn(true);

        underTest.saveDraft(CASE_REFERENCE, claim);

        verify(draftCaseDataService).saveUnsubmittedEventData(eq(CASE_REFERENCE), draftCaptor.capture(),
                                                              eq(citizenMakePossessionClaim), eq(3L));
        assertThat(draftCaptor.getValue().getCitizenPossessionClaim().getConfirmedSections())
            .isNotNull()
            .isEmpty();
    }

    @Test
    void shouldPassAStaleVersionConflictBackToTheCaller() {
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, citizenMakePossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.saveUnsubmittedEventData(eq(CASE_REFERENCE), any(PCSCase.class),
                                                           eq(citizenMakePossessionClaim), eq(2L)))
            .thenThrow(new DraftVersionConflictException(CASE_REFERENCE, 2L, 3L));

        CitizenPossessionClaim stale = CitizenPossessionClaim.builder().draftVersion(2L).build();
        assertThatThrownBy(() -> underTest.saveDraft(CASE_REFERENCE, stale))
            .isInstanceOf(DraftVersionConflictException.class);
    }

    private PcsCaseEntity caseWithAddress() {
        AddressEntity addressEntity = new AddressEntity();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setCaseReference(CASE_REFERENCE);
        caseEntity.setPropertyAddress(addressEntity);
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(ADDRESS);
        return caseEntity;
    }

    private void eligibility(LegislativeCountry provided, EligibilityStatus status, LegislativeCountry country) {
        when(eligibilityService.checkEligibility(POSTCODE, provided)).thenReturn(
            EligibilityResult.builder().status(status).legislativeCountry(country).build());
    }

    private void crossBorder() {
        when(eligibilityService.checkEligibility(POSTCODE, null)).thenReturn(
            EligibilityResult.builder()
                .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
                .legislativeCountries(List.of(LegislativeCountry.ENGLAND, LegislativeCountry.WALES))
                .build());
    }

}
