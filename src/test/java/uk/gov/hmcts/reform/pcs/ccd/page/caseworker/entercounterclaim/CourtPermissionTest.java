package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class CourtPermissionTest extends BasePageTest {

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2026, 8, 27);

    @Mock
    private Clock ukClock;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        lenient().when(partyService.buildPartyDynamicList(claimEntity, PartyRole.CLAIMANT))
            .thenReturn(DynamicList.builder().listItems(List.of()).build());
        lenient().when(partyService.buildPartyDynamicList(claimEntity, PartyRole.DEFENDANT))
            .thenReturn(DynamicList.builder().listItems(List.of()).build());

        setPageUnderTest(new CourtPermission(ukClock, pcsCaseService, partyService));
    }

    @ParameterizedTest
    @MethodSource("permissionOrderDateScenarios")
    void shouldRejectPermissionOrderDateInTheFutureWhenPermissionGranted(
            LocalDate permissionOrderDate, boolean isValid) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .courtPermissionGranted(VerticalYesNo.YES)
                .permissionOrderDate(permissionOrderDate)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (isValid) {
            assertThat(response.getErrorMessageOverride()).isNull();
        } else {
            assertThat(response.getErrorMessageOverride())
                .isEqualTo("Date the order was made must not be in the future");
        }
    }

    private static Stream<Arguments> permissionOrderDateScenarios() {
        return Stream.of(
            arguments(FIXED_CURRENT_DATE.plusDays(1), false),
            arguments(FIXED_CURRENT_DATE, true),
            arguments(FIXED_CURRENT_DATE.minusDays(1), true),
            arguments(FIXED_CURRENT_DATE.minusYears(5), true)
        );
    }

    @Test
    void shouldNotValidatePermissionOrderDateWhenPermissionNotGranted() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .courtPermissionGranted(VerticalYesNo.NO)
                .permissionOrderDate(FIXED_CURRENT_DATE.plusDays(1))
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
    }

    @ParameterizedTest
    @MethodSource("claimReceivedDateScenarios")
    void shouldRejectClaimReceivedDateInTheFuture(LocalDate claimReceivedDate, boolean isValid) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .claimReceivedDate(claimReceivedDate)
                .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        if (isValid) {
            assertThat(response.getErrorMessageOverride()).isNull();
        } else {
            assertThat(response.getErrorMessageOverride())
                .isEqualTo("Date the counterclaim was received must not be in the future");
        }
    }

    private static Stream<Arguments> claimReceivedDateScenarios() {
        return Stream.of(
            arguments(FIXED_CURRENT_DATE.plusDays(1), false),
            arguments(FIXED_CURRENT_DATE, true),
            arguments(FIXED_CURRENT_DATE.minusDays(1), true)
        );
    }

    @Test
    void shouldBuildAgainstPartyListExcludingSubmittingPartyInClaimantThenDefendantOrder() {
        // Given
        UUID claimantId = UUID.randomUUID();
        UUID defendant1Id = UUID.randomUUID();
        UUID defendant2Id = UUID.randomUUID();

        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.CLAIMANT)).thenReturn(
            DynamicList.builder().listItems(List.of(
                DynamicListElement.builder().code(claimantId).label("Jane Doe - Claimant 1").build()
            )).build());
        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.DEFENDANT)).thenReturn(
            DynamicList.builder().listItems(List.of(
                DynamicListElement.builder().code(defendant1Id).label("John Smith - Defendant 1").build(),
                DynamicListElement.builder().code(defendant2Id).label("Alex Brown - Defendant 2").build()
            )).build());

        DynamicList submittingPartyList = DynamicList.builder()
            .value(DynamicListElement.builder().code(defendant1Id).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .partyRadioList(submittingPartyList)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        List<DynamicStringListElement> listItems = response.getData().getPartyMultiSelectionList().getListItems();
        assertThat(listItems)
            .extracting(DynamicStringListElement::getCode, DynamicStringListElement::getLabel)
            .containsExactly(
                tuple(claimantId.toString(), "Jane Doe - Claimant 1"),
                tuple(defendant2Id.toString(), "Alex Brown - Defendant 2")
            );
    }

    @Test
    void shouldIncludeAllPartiesWhenSubmittingPartyNotYetSelected() {
        // Given
        UUID claimantId = UUID.randomUUID();

        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.CLAIMANT)).thenReturn(
            DynamicList.builder().listItems(List.of(
                DynamicListElement.builder().code(claimantId).label("Jane Doe - Claimant 1").build()
            )).build());

        PCSCase caseData = PCSCase.builder().build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getPartyMultiSelectionList().getListItems())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(claimantId.toString());
    }
}
