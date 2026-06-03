package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.SearchCriteria;
import uk.gov.hmcts.ccd.sdk.type.SearchParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.view.AlternativesToPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.AsbProhibitedConductView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseLinkView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseNoteView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimGroundsView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimView;
import uk.gov.hmcts.reform.pcs.ccd.view.GenAppsView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.PartiesView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentDetailsView;
import uk.gov.hmcts.reform.pcs.ccd.view.StatementOfTruthView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.CaseFieldsView;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@ExtendWith(MockitoExtension.class)
class PCSCaseViewTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final State DEFAULT_STATE = State.CASE_ISSUED;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private CaseTitleService caseTitleService;
    @Mock
    private ClaimView claimView;
    @Mock
    private TenancyLicenceView tenancyLicenceView;
    @Mock
    private ClaimGroundsView claimGroundsView;
    @Mock
    private RentDetailsView rentDetailsView;
    @Mock
    private AlternativesToPossessionView alternativesToPossessionView;
    @Mock
    private AsbProhibitedConductView asbProhibitedConductView;
    @Mock
    private RentArrearsView rentArrearsView;
    @Mock
    private NoticeOfPossessionView noticeOfPossessionView;
    @Mock
    private StatementOfTruthView statementOfTruthView;
    @Mock
    private GenAppsView genAppsView;

    @Mock(strictness = LENIENT)
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private CaseFieldsView caseFieldsView;
    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;
    @Mock
    private CaseLinkView caseLinkView;
    @Mock
    private CaseNoteView caseNoteView;
    @Mock
    private CaseTabView caseTabView;
    @Mock
    private PartiesView partiesView;
    @Mock
    private CaseFlagsView caseFlagsView;

    private PCSCaseView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        underTest = new PCSCaseView(pcsCaseRepository, securityContextService, modelMapper, draftCaseDataService,
                                    caseTitleService, claimView, tenancyLicenceView, claimGroundsView, rentDetailsView,
                                    alternativesToPossessionView, asbProhibitedConductView,
                                    rentArrearsView, noticeOfPossessionView,
                                    statementOfTruthView, caseFieldsView, caseLinkView, enforcementOrderMediator,
                                    caseNoteView, caseTabView, partiesView, genAppsView, caseFlagsView
        );
    }

    @Test
    void shouldThrowExceptionForUnknownCaseReference() {
        // Given
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        CaseViewRequest<State> request = request(CASE_REFERENCE, DEFAULT_STATE);

        // Then
        assertThatThrownBy(() -> underTest.getCase(request))
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldReturnCaseWithNoPropertyAddress() {
        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getPropertyAddress()).isNull();
    }

    @Test
    void shouldSetCaseTitleMarkdown() {
        // Given
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);

        String expectedCaseTitle = "expected case title";
        when(caseTitleService.buildCaseTitle(any(PCSCase.class))).thenReturn(expectedCaseTitle);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getCaseTitleMarkdown()).isEqualTo(expectedCaseTitle);

        ArgumentCaptor<PCSCase> pcsCaseCaptor = ArgumentCaptor.forClass(PCSCase.class);
        verify(caseTitleService).buildCaseTitle(pcsCaseCaptor.capture());
        assertThat(pcsCaseCaptor.getValue().getPropertyAddress()).isEqualTo(addressUK);
    }

    @Test
    void shouldMapPropertyAddress() {
        // Given
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getPropertyAddress()).isEqualTo(addressUK);
    }

    @Test
    void shouldIndexPropertyPostcodeAsSearchPartyWithoutPollutingParties() {
        // Given - a property address with a postcode and no real parties
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);
        when(addressUK.getPostCode()).thenReturn("AB1 2CD");
        when(addressUK.getAddressLine1()).thenReturn("1 Test Street");

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then - the property postcode is indexed as a SearchParty for global search
        SearchCriteria searchCriteria = pcsCase.getSearchCriteria();
        assertThat(searchCriteria.getParties())
            .extracting(ListValue::getValue)
            .extracting(SearchParty::getPostcode)
            .contains("AB1 2CD");

        // And the property is NOT added to the real parties list (no downstream pollution)
        assertThat(pcsCase.getParties()).isEmpty();
    }

    @Test
    void shouldIndexPropertyAddressLine1AndPostcodeOnSearchParty() {
        // Given - a property address with both an address line and a postcode
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);
        when(addressUK.getPostCode()).thenReturn("AB1 2CD");
        when(addressUK.getAddressLine1()).thenReturn("1 Test Street");

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then - both fields are copied onto the property SearchParty
        assertThat(pcsCase.getSearchCriteria().getParties())
            .extracting(ListValue::getValue)
            .anySatisfy(searchParty -> {
                assertThat(searchParty.getAddressLine1()).isEqualTo("1 Test Street");
                assertThat(searchParty.getPostcode()).isEqualTo("AB1 2CD");
            });
    }

    @Test
    void shouldNotIndexPropertyWhenAddressIsNull() {
        // Given - no property address (pcsCaseEntity.getPropertyAddress() returns null)

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then - no property SearchParty is added
        assertThat(pcsCase.getSearchCriteria().getParties()).isEmpty();
    }

    @Test
    void shouldNotIndexPropertyWhenPostcodeIsNull() {
        // Given - a property address with no postcode
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        stubAddressEntityModelMapper(addressEntity);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then - no property SearchParty is added
        assertThat(pcsCase.getSearchCriteria().getParties()).isEmpty();
    }

    @Test
    void shouldNotIndexPropertyWhenPostcodeIsBlank() {
        // Given - a property address with a blank postcode
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);
        when(addressUK.getPostCode()).thenReturn("   ");

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then - no property SearchParty is added
        assertThat(pcsCase.getSearchCriteria().getParties()).isEmpty();
    }

    @Test
    void shouldMapPartyEntity() {
        // Given
        PartyEntity partyEntity = mock(PartyEntity.class);
        when(pcsCaseEntity.getParties()).thenReturn(Set.of(partyEntity));

        Party party = mock(Party.class);

        when(modelMapper.map(partyEntity, Party.class)).thenReturn(party);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        List<ListValue<Party>> mappedParties = pcsCase.getParties();
        assertThat(mappedParties).hasSize(1);
        assertThat(mappedParties.getFirst().getValue()).isSameAs(party);
    }

    @Test
    void shouldMapLegislativeCountry() {
        // Given
        LegislativeCountry expectedLegislativeCountry = LegislativeCountry.SCOTLAND;
        when(pcsCaseEntity.getLegislativeCountry()).thenReturn(expectedLegislativeCountry);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(expectedLegislativeCountry);
    }

    @Test
    void shouldMapDateSubmittedFromClaimSubmittedDate() {
        // Given
        LocalDateTime claimSubmittedDate = LocalDateTime.of(2026, 5, 12, 14, 30);
        when(claimEntity.getClaimSubmittedDate()).thenReturn(claimSubmittedDate);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getDateSubmitted()).isEqualTo(claimSubmittedDate);
    }

    @Test
    void shouldReturnEmptyListWhenNoDocumentsExist() {
        // Given
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of());

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getAllDocuments()).isEmpty();
    }

    @Test
    void shouldMapDocuments() {
        // Given
        Instant submittedDate = Instant.parse("2026-05-14T09:30:00Z");
        DocumentEntity entity1 = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName("doc1.pdf")
            .url("url1")
            .submittedDate(submittedDate)
            .build();

        DocumentEntity entity2 = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName("doc2.pdf")
            .url("url2")
            .build();

        when(pcsCaseEntity.getDocuments()).thenReturn(List.of(entity1,entity2));

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        //Then
        assertThat(pcsCase.getAllDocuments()).hasSize(2);
        assertThat(pcsCase.getAllDocuments()).extracting(lv -> lv.getValue().getFilename())
            .containsExactly("doc1.pdf", "doc2.pdf");
        assertThat(pcsCase.getAllDocuments()).extracting(lv -> lv.getValue().getUploadTimestamp())
            .containsExactly(LocalDateTime.of(2026, 5, 14, 9, 30), null);
    }

    private static ListValue<Party> asListValue(UUID id, Party party) {
        return ListValue.<Party>builder().id(id.toString()).value(party).build();
    }

    private ClaimPartyEntity createClaimPartyEntity(Party party, UUID partyId, PartyRole partyRole) {
        PartyEntity partyEntity = mock(PartyEntity.class);

        when(modelMapper.map(partyEntity, Party.class)).thenReturn(party);

        ClaimPartyId claimPartyId = new ClaimPartyId();
        claimPartyId.setPartyId(partyId);

        return ClaimPartyEntity.builder()
            .id(claimPartyId)
            .role(partyRole)
            .party(partyEntity)
            .build();
    }

    @Test
    void shouldSetCaseFieldsInViewHelpers() {
        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        verify(partiesView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(claimView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(tenancyLicenceView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(claimGroundsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(rentDetailsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(alternativesToPossessionView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(asbProhibitedConductView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(rentArrearsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(noticeOfPossessionView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(statementOfTruthView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(caseLinkView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(caseFlagsView).setCaseFields(pcsCase, pcsCaseEntity);
    }

    @Test
    void shouldSetCaseFields() {
        // When
        doNothing().when(caseFieldsView).setCaseFields(any(PCSCase.class));

        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        verify(caseFieldsView).setCaseFields(pcsCase);
    }

    @Test
    void shouldSetDraftCaseTabFieldsWhenUnsubmittedCaseDataExists() {
        // Given
        PCSCase draftCaseData = PCSCase.builder().build();
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim))
            .thenReturn(Optional.of(draftCaseData));

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, State.AWAITING_SUBMISSION_TO_HMCTS));

        // Then
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim);
        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim);
        verify(caseTabView).setDraftCaseTabFields(pcsCase, draftCaseData);
        verify(caseTabView, never()).setCaseTabFields(any(PCSCase.class));
        assertThat(pcsCase.getNextStepsMarkdown()).contains("Resume claim");
    }

    @Test
    void shouldSetSubmittedCaseTabFieldsWhenUnsubmittedCaseDataIsExpectedButDraftIsMissing() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim))
            .thenReturn(Optional.empty());

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, State.AWAITING_SUBMISSION_TO_HMCTS));

        // Then
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim);
        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim);
        verify(caseTabView, never()).setDraftCaseTabFields(any(PCSCase.class), any(PCSCase.class));
        verify(caseTabView).setCaseTabFields(pcsCase);
        assertThat(pcsCase.getNextStepsMarkdown()).contains("Resume claim");
    }

    @Test
    void shouldNotFetchUnsubmittedCaseDataWhenNoUnsubmittedCaseDataExists() {
        // When
        underTest.getCase(request(CASE_REFERENCE, State.AWAITING_SUBMISSION_TO_HMCTS));

        // Then
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, resumePossessionClaim);
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(any(Long.class), any());
        verify(caseTabView, never()).setDraftCaseTabFields(any(PCSCase.class), any(PCSCase.class));
        verify(caseTabView).setCaseTabFields(any(PCSCase.class));
    }

    @Test
    void shouldNotLoadUnsubmittedCaseDataWhenCaseIsNotAwaitingSubmission() {
        // When
        underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        verify(draftCaseDataService, never()).getUnsubmittedCaseData(any(Long.class), any());
    }

    @Test
    void shouldCallEnforcementOrderMediator() {
        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        verify(enforcementOrderMediator).handleEnforcementRequirements(CASE_REFERENCE, pcsCase);
    }

    private AddressUK stubAddressEntityModelMapper(AddressEntity addressEntity) {
        AddressUK addressUK = mock(AddressUK.class);
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(addressUK);
        return addressUK;
    }

    @SuppressWarnings("SameParameterValue")
    private static CaseViewRequest<State> request(long caseReference, State state) {
        return new CaseViewRequest<>(caseReference, state);
    }

}
