package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.LegalCosts;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcement.LegalCostsPage.VALID_AMOUNT_ERROR_MESSAGE;

class LegalCostsPageTest extends BasePageTest {

    private LegalCostsPage underTest;
    private PCSCase pcsCase;
    private LegalCosts legalCosts;

    @BeforeEach
    void beforeEach() {
        pcsCase = mock(PCSCase.class);
        EnforcementOrder enforcementOrder = mock(EnforcementOrder.class);
        legalCosts = mock(LegalCosts.class);
        when(pcsCase.getEnforcementOrder()).thenReturn(enforcementOrder);
        when(enforcementOrder.getLegalCosts()).thenReturn(legalCosts);
        underTest = new LegalCostsPage();
        setPageUnderTest(underTest);
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.50", "100.123", "100", "212.987"})
    void shouldReturnEmptyListWhenAmountOfLegalCostsIsValid(String amount) {
        // Given
        when(legalCosts.getAreLegalCostsToBeClaimed()).thenReturn(VerticalYesNo.YES);
        when(legalCosts.getAmountOfLegalCosts()).thenReturn(amount);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0.00", "-50.00", "100.50.25", "abc"})
    void shouldReturnErrorMessageWhenAmountOfLegalCostsIsInvalid(String amount) {
        // Given
        when(legalCosts.getAreLegalCostsToBeClaimed()).thenReturn(VerticalYesNo.YES);
        when(legalCosts.getAmountOfLegalCosts()).thenReturn(amount);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).containsExactly(VALID_AMOUNT_ERROR_MESSAGE);
    }

    @Test
    void shouldReturnEmptyListWhenAreLegalCostsToBeClaimedIsNo() {
        // Given
        when(legalCosts.getAreLegalCostsToBeClaimed()).thenReturn(VerticalYesNo.NO);

        // When
        List<String> errors = underTest.validateUserInput(pcsCase);

        // Then
        assertThat(errors).isEmpty();
    }

}
