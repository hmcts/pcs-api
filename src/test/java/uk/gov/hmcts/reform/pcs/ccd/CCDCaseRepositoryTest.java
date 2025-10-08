package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCDCaseRepositoryTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String STATE = "some state";

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private UnsubmittedCaseDataService unsubmittedCaseDataService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

    private CCDCaseRepository underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        underTest = new CCDCaseRepository(pcsCaseRepository, securityContextService,
                modelMapper, pcsCaseService, unsubmittedCaseDataService);
    }

    @Test
    void shouldThrowExceptionForUnknownCaseReference() {
        // Given
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.getCase(CASE_REFERENCE, STATE));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource("unsubmittedDataFlagScenarios")
    void shouldSetFlagForUnsubmittedData(boolean hasUnsubmittedData, YesOrNo expectedCaseDataValue) {
        // Given
        when(unsubmittedCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE)).thenReturn(hasUnsubmittedData);

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, State.AWAITING_FURTHER_CLAIM_DETAILS.name());

        // Then
        assertThat(pcsCase.getHasUnsubmittedCaseData()).isEqualTo(expectedCaseDataValue);
    }

    private static Stream<Arguments> unsubmittedDataFlagScenarios() {
        return Stream.of(
            // unsubmitted case data available, expected case data value
            arguments(false, YesOrNo.NO),
            arguments(true, YesOrNo.YES)
        );
    }

    @Test
    void shouldReturnCaseWithNoPropertyAddress() {
        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getPropertyAddress()).isNull();
    }

    @Test
    void shouldSetPageHeadingMarkdownWhenCaseIsRetrieved() {
        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getPageHeadingMarkdown()).isNotBlank();
    }

    @Test
    void shouldSetPageHeadingMarkdownWhenCaseIsRetrieved() {
        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getPageHeadingMarkdown()).isNotBlank();
    }

    @Test
    void shouldMapPropertyAddress() {
        // Given
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

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
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        List<ListValue<Party>> mappedParties = pcsCase.getParties();
        assertThat(mappedParties).hasSize(1);
        assertThat(mappedParties.getFirst().getValue()).isSameAs(party);
    }

    @Test
    void shouldMapPreActionProtocolCompletedWhenYes() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPreActionProtocolCompleted()).thenReturn(true);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
    }

    @ParameterizedTest
    @MethodSource("preActionProtocolScenarios")
    void shouldMapPreActionProtocolCompleted(Boolean databaseFlag,
                                             VerticalYesNo expectedCaseDataValue) {
        // Given
        when(pcsCaseEntity.getPreActionProtocolCompleted()).thenReturn(databaseFlag);

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(expectedCaseDataValue);
    }

    private static Stream<Arguments> preActionProtocolScenarios() {
        return Stream.of(
            // DB value, expected case data value
            arguments(false, VerticalYesNo.NO),
            arguments(true, VerticalYesNo.YES),
            arguments(null, null)
        );
    }

    @Test
    void shouldMapLegislativeCountry() {
        // Given
        LegislativeCountry expectedLegislativeCountry = LegislativeCountry.SCOTLAND;
        when(pcsCaseEntity.getLegislativeCountry()).thenReturn(expectedLegislativeCountry);

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE, STATE);

        // Then
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(expectedLegislativeCountry);
    }

    private AddressUK stubAddressEntityModelMapper(AddressEntity addressEntity) {
        AddressUK addressUK = mock(AddressUK.class);
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(addressUK);
        return addressUK;
    }

}
