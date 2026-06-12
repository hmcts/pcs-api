package uk.gov.hmcts.reform.pcs.ccd.event.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.service.LegalRepresentativeService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private OrganisationService organisationService;
    @Mock
    private LegalRepresentativeService legalRepresentativeService;
    @Mock
    private FeeApplier feeApplier;
    @Captor
    private ArgumentCaptor<BiConsumer<PCSCase, String>> feeSetterCaptor;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(organisationService, legalRepresentativeService, feeApplier);
    }

    @Test
    void shouldSetRepresentedPartiesFieldWhenUserRepresentsOne() {
        // Given
        UUID expectedPartyId = UUID.randomUUID();
        String expectedPartyName = "Some party name";
        DynamicList expectedPartyNameList = DynamicList.builder()
            .listItems(List.of(DynamicListElement.builder()
                                   .code(expectedPartyId)
                                   .label(expectedPartyName)
                                   .build()))
            .build();

        String orgId = UUID.randomUUID().toString();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(orgId);
        when(legalRepresentativeService.getRepresentedPartiesDynamicList(orgId, TEST_CASE_REFERENCE))
            .thenReturn(Optional.of(expectedPartyNameList));

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        underTest.start(eventPayload(caseData));

        // Then
        assertThat(caseData.getRepresentedPartyNames()).isEqualTo(expectedPartyNameList);
        assertThat(caseData.getCurrentRepresentedPartyId()).isEqualTo(expectedPartyId.toString());
        assertThat(caseData.getCurrentRepresentedPartyName()).isEqualTo(expectedPartyName);
    }

    @ParameterizedTest
    @MethodSource("multipleRepresentedPartiesScenarios")
    void shouldSetMultipleRepresentedPartiesFlag(int numRepresentedParties, VerticalYesNo expectedFlag) {
        // Given
        List<DynamicListElement> listItems = IntStream.range(0, numRepresentedParties)
            .boxed()
            .map(i -> DynamicListElement.builder().code(UUID.randomUUID()).build())
            .toList();

        DynamicList expectedPartyNameList = DynamicList.builder()
            .listItems(listItems)
            .build();

        String orgId = UUID.randomUUID().toString();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(orgId);
        when(legalRepresentativeService.getRepresentedPartiesDynamicList(orgId, TEST_CASE_REFERENCE))
            .thenReturn(Optional.of(expectedPartyNameList));

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        underTest.start(eventPayload(caseData));

        // Then
        assertThat(caseData.getMultipleRepresentedParties()).isEqualTo(expectedFlag);
    }

    private static Stream<Arguments> multipleRepresentedPartiesScenarios() {
        return Stream.of(
            // Represented party count, expected flag
            arguments(0, VerticalYesNo.NO),
            arguments(1, VerticalYesNo.NO),
            arguments(2, VerticalYesNo.YES)
        );
    }

    @Test
    void shouldNotSetRepresentedPartiesFieldWhenUserDoesNotRepresentAny() {
        // Given
        String orgId = UUID.randomUUID().toString();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(orgId);
        when(legalRepresentativeService.getRepresentedPartiesDynamicList(orgId, TEST_CASE_REFERENCE))
            .thenReturn(Optional.empty());

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        underTest.start(eventPayload(caseData));

        // Then
        assertThat(caseData.getRepresentedPartyNames()).isNull();
    }

    @Test
    void shouldSetTheApplicationFees() {
        // Given
        final String formattedStandardFee = "10.99";
        final String formattedMaxFee = "20.99";

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        underTest.start(eventPayload(caseData));

        // Then
        verify(feeApplier)
            .applyFeeAmount(eq(caseData), eq(FeeType.GEN_APP_STANDARD_FEE), feeSetterCaptor.capture());
        feeSetterCaptor.getValue().accept(caseData, formattedStandardFee);
        assertThat(caseData.getXuiGenAppRequest().getStandardFee()).isEqualTo(formattedStandardFee);

        verify(feeApplier)
            .applyFeeAmount(eq(caseData), eq(FeeType.GEN_APP_MAX_FEE), feeSetterCaptor.capture());
        feeSetterCaptor.getValue().accept(caseData, formattedMaxFee);
        assertThat(caseData.getXuiGenAppRequest().getMaxFee()).isEqualTo(formattedMaxFee);
    }

    @Test
    void shouldSetShowHwfScreensFlagToYes() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().build())
            .build();

        // When
        underTest.start(eventPayload(caseData));

        // Then
        assertThat(caseData.getXuiGenAppRequest().getShowHwfScreens()).isEqualTo(VerticalYesNo.YES);
    }

    private static EventPayload<PCSCase, State> eventPayload(PCSCase caseData) {
        return new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
    }

}
