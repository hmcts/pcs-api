package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LandRegistryFees;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcement.LandRegistryFeesPage.VALID_AMOUNT_ERROR_MESSAGE;


class LandRegistryFeesPageTest {

    private LandRegistryFeesPage underTest;
    private PCSCase pcsCase;
    private LandRegistryFees landRegistryFees;

    @BeforeEach
    void beforeEach() {
        pcsCase = mock(PCSCase.class);
        EnforcementOrder enforcementOrder = mock(EnforcementOrder.class);
        landRegistryFees = mock(LandRegistryFees.class);
        when(pcsCase.getEnforcementOrder()).thenReturn(enforcementOrder);
        when(enforcementOrder.getLandRegistryFees()).thenReturn(landRegistryFees);
        underTest = new LandRegistryFeesPage();
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.50", "100.123", "100", "212.987"})
    void shouldReturnEmptyListWhenAmountOfLandRegistryFeesIsValid(String amount) {
        // Given
        when(landRegistryFees.getHaveLandRegistryFeesBeenPaid()).thenReturn(VerticalYesNo.YES);
        when(landRegistryFees.getAmountOfLandRegistryFees()).thenReturn(amount);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0.00", "-50.00", "100.50.25", "abc"})
    void shouldReturnErrorMessageWhenAmountOfLandRegistryFeesIsInvalid(String amount) {
        // Given
        when(landRegistryFees.getHaveLandRegistryFeesBeenPaid()).thenReturn(VerticalYesNo.YES);
        when(landRegistryFees.getAmountOfLandRegistryFees()).thenReturn(amount);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).containsExactly(VALID_AMOUNT_ERROR_MESSAGE);
    }

    @Test
    void shouldReturnEmptyListWhenAreLandRegistryFeesToBeClaimedIsNo() {
        // Given
        when(landRegistryFees.getHaveLandRegistryFeesBeenPaid()).thenReturn(VerticalYesNo.NO);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).isEmpty();
    }
}
