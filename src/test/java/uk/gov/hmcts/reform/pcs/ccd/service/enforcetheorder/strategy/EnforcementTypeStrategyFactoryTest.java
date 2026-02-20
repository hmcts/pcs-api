package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementTypeStrategyFactoryTest {

    @Mock
    private WarrantStrategy warrantStrategy;
    @Mock
    private WritStrategy writStrategy;

    @InjectMocks
    private EnforcementTypeStrategyFactory underTest;

    @Test
    void shouldReturnWarrantStrategyForWarrantType() {
        // When
        EnforcementTypeStrategy strategy = underTest.getStrategy(SelectEnforcementType.WARRANT);

        // Then
        assertThat(strategy).isSameAs(warrantStrategy);
    }

    @Test
    void shouldReturnWritStrategyForWritType() {
        // When
        EnforcementTypeStrategy strategy = underTest.getStrategy(SelectEnforcementType.WRIT);

        // Then
        assertThat(strategy).isSameAs(writStrategy);
    }

    @Test
    void shouldReturnDifferentStrategiesForDifferentTypes() {
        // When
        EnforcementTypeStrategy warrantResult = underTest.getStrategy(SelectEnforcementType.WARRANT);
        EnforcementTypeStrategy writResult = underTest.getStrategy(SelectEnforcementType.WRIT);

        // Then
        assertThat(warrantResult).isNotSameAs(writResult);
        assertThat(warrantResult).isSameAs(warrantStrategy);
        assertThat(writResult).isSameAs(writStrategy);
    }

    @Test
    void shouldConsistentlyReturnSameStrategyInstance() {
        // When
        EnforcementTypeStrategy firstCall = underTest.getStrategy(SelectEnforcementType.WARRANT);
        EnforcementTypeStrategy secondCall = underTest.getStrategy(SelectEnforcementType.WARRANT);

        // Then
        assertThat(firstCall).isSameAs(secondCall);
        assertThat(firstCall).isSameAs(warrantStrategy);
    }
}
