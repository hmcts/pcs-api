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
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private DefendantService defendantService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;

    private PCSCaseView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        underTest = new PCSCaseView(pcsCaseRepository, securityContextService,
                modelMapper, draftCaseDataService, caseTitleService, defendantService);
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
    void shouldMapDefendants() {
        // Given
        List<Defendant> defendantList = List.of(mock(Defendant.class), mock(Defendant.class));
        DefendantDetails defendantDetails1 = mock(DefendantDetails.class);
        DefendantDetails defendantDetails2 = mock(DefendantDetails.class);

        when(pcsCaseEntity.getDefendants()).thenReturn(defendantList);
        when(defendantService.mapToDefendantDetails(defendantList))
            .thenReturn(List.of(defendantDetails1, defendantDetails2));

        // When
        PCSCase pcsCase = underTest.getCase(request(CASE_REFERENCE, DEFAULT_STATE));

        // Then
        assertThat(pcsCase.getAllDefendants())
            .map(ListValue::getValue)
            .containsExactly(defendantDetails1, defendantDetails2);
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
