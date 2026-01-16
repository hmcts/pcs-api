package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.view.AlternativesToPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.HousingActWalesView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentDetailsView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

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
    private TenancyLicenceView tenancyLicenceView;
    @Mock
    private RentDetailsView rentDetailsView;
    @Mock
    private AlternativesToPossessionView alternativesToPossessionView;
    @Mock
    private HousingActWalesView housingActWalesView;
    @Mock(strictness = LENIENT)
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;

    private PCSCaseView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        underTest = new PCSCaseView(pcsCaseRepository, securityContextService,
                                    modelMapper, draftCaseDataService, caseTitleService,
                                    tenancyLicenceView, rentDetailsView, alternativesToPossessionView,
                                    housingActWalesView
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
    void shouldMapBasicClaimFields() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getAgainstTrespassers()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(claimEntity.getClaimCosts()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getPreActionProtocolFollowed()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getMediationAttempted()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getMediationDetails()).thenReturn("mediation details");
        when(claimEntity.getSettlementAttempted()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getSettlementDetails()).thenReturn("settlement details");
        when(claimEntity.getAdditionalDefendants()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getAdditionalUnderlesseesOrMortgagees()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getGenAppExpected()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        when(claimEntity.getAdditionalDocsProvided()).thenReturn(VerticalYesNo.YES);

        // When
        PCSCase result = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(result.getClaimAgainstTrespassers()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getClaimDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(result.getClaimingCostsWanted()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getMediationAttempted()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getMediationAttemptedDetails()).isEqualTo("mediation details");
        assertThat(result.getSettlementAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getSettlementAttemptedDetails()).isEqualTo("settlement details");
        assertThat(result.getAddAnotherDefendant()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getHasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getAddAdditionalUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
        assertThat(result.getApplicationWithClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(result.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(result.getWantToUploadDocuments()).isEqualTo(VerticalYesNo.YES);
    }

    @ParameterizedTest
    @MethodSource("complexClaimFieldsScenarios")
    void shouldMapComplexClaimFields(
        VerticalYesNo claimantSelect,
        String claimantDetails,
        VerticalYesNo defendantSelect,
        String defendantDetails,
        VerticalYesNo additionalReasonsProvided,
        String additionalReasons,
        ClaimantType claimantType
    ) {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getClaimantCircumstancesProvided()).thenReturn(claimantSelect);
        when(claimEntity.getClaimantCircumstances()).thenReturn(claimantDetails);

        when(claimEntity.getDefendantCircumstancesProvided()).thenReturn(defendantSelect);
        when(claimEntity.getDefendantCircumstances()).thenReturn(defendantDetails);

        when(claimEntity.getAdditionalReasonsProvided()).thenReturn(additionalReasonsProvided);
        when(claimEntity.getAdditionalReasons()).thenReturn(additionalReasons);

        when(claimEntity.getClaimantType()).thenReturn(claimantType);

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesSelect()).isEqualTo(claimantSelect);
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesDetails()).isEqualTo(claimantDetails);

        assertThat(pcsCase.getDefendantCircumstances().getHasDefendantCircumstancesInfo()).isEqualTo(defendantSelect);
        assertThat(pcsCase.getDefendantCircumstances().getDefendantCircumstancesInfo()).isEqualTo(defendantDetails);

        assertThat(pcsCase.getAdditionalReasonsForPossession().getHasReasons()).isEqualTo(additionalReasonsProvided);
        assertThat(pcsCase.getAdditionalReasonsForPossession().getReasons()).isEqualTo(additionalReasons);

        if (claimantType != null) {
            assertThat(pcsCase.getClaimantType().getValue().getCode()).isEqualTo(claimantType.name());
            assertThat(pcsCase.getClaimantType().getValue().getLabel()).isEqualTo(claimantType.getLabel());
        } else {
            assertThat(pcsCase.getClaimantType()).isNull();
        }
    }

    @Test
    void shouldNotPopulateAnyClaimFieldsWhenNoClaimsExist() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        assertThat(pcsCase.getClaimAgainstTrespassers()).isNull();
        assertThat(pcsCase.getClaimingCostsWanted()).isNull();
        assertThat(pcsCase.getClaimantCircumstances()).isNull();
        assertThat(pcsCase.getDefendantCircumstances()).isNull();
        assertThat(pcsCase.getAdditionalReasonsForPossession()).isNull();
        assertThat(pcsCase.getClaimantType()).isNull();
    }

    @Test
    void shouldSetCaseFieldsInViewHelpers() {
        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        verify(tenancyLicenceView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(rentDetailsView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(alternativesToPossessionView).setCaseFields(pcsCase, pcsCaseEntity);
        verify(housingActWalesView).setCaseFields(pcsCase, pcsCaseEntity);
    }

    private static Stream<Arguments> complexClaimFieldsScenarios() {
        return Stream.of(
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.PRIVATE_LANDLORD
            ),
            Arguments.of(
                null, null,
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.COMMUNITY_LANDLORD
            ),
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                null, null,
                VerticalYesNo.NO, null,
                ClaimantType.MORTGAGE_LENDER
            ),
            Arguments.of(
                VerticalYesNo.NO, "some claimant details",
                VerticalYesNo.YES, "some defendant details",
                VerticalYesNo.NO, "reasons",
                null
            )
        );
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
