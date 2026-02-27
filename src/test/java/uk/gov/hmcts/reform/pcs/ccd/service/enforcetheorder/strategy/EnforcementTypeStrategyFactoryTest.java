
package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementTypeStrategyFactoryTest {

    @Mock
    private WarrantStrategy warrantStrategy;
    @Mock
    private WritStrategy writStrategy;
    @Mock
    private WritOfRestitutionStrategy writOfRestitutionStrategy;

    @InjectMocks
    private EnforcementTypeStrategyFactory underTest;

    @ParameterizedTest
    @MethodSource("provideEnforcementTypeAndExpectedStrategy")
    void shouldReturnCorrectStrategyForEnforcementType(SelectEnforcementType type, String expectedStrategy) {
        // When
        EnforcementTypeStrategy strategy = underTest.getStrategy(type);

        // Then
        assertThat(strategy.getClass().getSimpleName()).isEqualTo(expectedStrategy);
    }

    private static Stream<Arguments> provideEnforcementTypeAndExpectedStrategy() {
        return Stream.of(
            Arguments.of(SelectEnforcementType.WARRANT, WarrantStrategy.class.getSimpleName()),
            Arguments.of(SelectEnforcementType.WRIT, WritStrategy.class.getSimpleName()),
            Arguments.of(SelectEnforcementType.WRIT_OF_RESTITUTION,
                         WritOfRestitutionStrategy.class.getSimpleName())
        );
    }

    @Test
    void shouldReturnDifferentStrategiesForDifferentTypes() {
        // When
        EnforcementTypeStrategy warrantResult = underTest.getStrategy(SelectEnforcementType.WARRANT);
        EnforcementTypeStrategy writResult = underTest.getStrategy(SelectEnforcementType.WRIT);
        EnforcementTypeStrategy writOfRestitutionResult = underTest
            .getStrategy(SelectEnforcementType.WRIT_OF_RESTITUTION);

        // Then
        assertThat(warrantResult).isSameAs(warrantStrategy);
        assertThat(writResult).isSameAs(writStrategy);
        assertThat(writOfRestitutionResult).isSameAs(writOfRestitutionStrategy);
    }

}
