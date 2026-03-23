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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
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
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimGroundsView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimView;
import uk.gov.hmcts.reform.pcs.ccd.view.HousingActWalesView;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentDetailsView;
import uk.gov.hmcts.reform.pcs.ccd.view.StatementOfTruthView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private HousingActWalesView housingActWalesView;
    @Mock
    private AsbProhibitedConductView asbProhibitedConductView;
    @Mock
    private RentArrearsView rentArrearsView;
    @Mock
    private NoticeOfPossessionView noticeOfPossessionView;
    @Mock
    private StatementOfTruthView statementOfTruthView;

    @Mock(strictness = LENIENT)
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;

    private PCSCaseView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        underTest = new PCSCaseView(pcsCaseRepository, securityContextService, modelMapper, draftCaseDataService,
                                    caseTitleService, claimView, tenancyLicenceView, claimGroundsView, rentDetailsView,
                                    alternativesToPossessionView, housingActWalesView, asbProhibitedConductView,
                                    rentArrearsView, noticeOfPossessionView, statementOfTruthView
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
    void shouldMapAllParties() {
        // Given
        Party claimant = mock(Party.class);
        UUID claimantId = UUID.randomUUID();
        ClaimPartyEntity claimantClaimParty = createClaimPartyEntity(claimant, claimantId, PartyRole.CLAIMANT);

        Party defendant1 = mock(Party.class);
        UUID defendant1Id = UUID.randomUUID();
        ClaimPartyEntity defendant1ClaimParty = createClaimPartyEntity(defendant1, defendant1Id, PartyRole.DEFENDANT);

        Party defendant2 = mock(Party.class);
        UUID defendant2Id = UUID.randomUUID();
        ClaimPartyEntity defendant2ClaimParty = createClaimPartyEntity(defendant2, defendant2Id, PartyRole.DEFENDANT);

        Party underlessee1 = mock(Party.class);
        UUID underlessee1Id = UUID.randomUUID();
        ClaimPartyEntity underlessee1ClaimParty = createClaimPartyEntity(
            underlessee1,
            underlessee1Id,
            PartyRole.UNDERLESSEE_OR_MORTGAGEE
        );

        Party underlessee2 = mock(Party.class);
        UUID underlessee2Id = UUID.randomUUID();
        ClaimPartyEntity underlessee2ClaimParty = createClaimPartyEntity(
            underlessee2,
            underlessee2Id,
            PartyRole.UNDERLESSEE_OR_MORTGAGEE
        );

        when(claimEntity.getClaimParties()).thenReturn(
            List.of(claimantClaimParty, defendant1ClaimParty, defendant2ClaimParty,
                    underlessee1ClaimParty, underlessee2ClaimParty
            ));

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getAllClaimants())
            .containsExactly(asListValue(claimantId, claimant));

        assertThat(pcsCase.getAllDefendants())
            .containsExactly(
                asListValue(defendant1Id, defendant1),
                asListValue(defendant2Id, defendant2)
            );

        assertThat(pcsCase.getAllUnderlesseeOrMortgagees())
            .containsExactly(
                asListValue(underlessee1Id, underlessee1),
                asListValue(underlessee2Id, underlessee2)
            );

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
        DocumentEntity entity1 = DocumentEntity.builder()
            .id(UUID.randomUUID())
            .fileName("doc1.pdf")
            .url("url1")
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
        verify(claimView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(tenancyLicenceView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(claimGroundsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(rentDetailsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(alternativesToPossessionView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(housingActWalesView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(asbProhibitedConductView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(rentArrearsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(noticeOfPossessionView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(statementOfTruthView).setCaseFields(pcsCase, pcsCaseEntity);
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
