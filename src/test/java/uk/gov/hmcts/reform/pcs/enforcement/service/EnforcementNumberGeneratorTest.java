package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementNumberGeneratorTest {

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @InjectMocks
    private EnforcementNumberGenerator underTest;

    @ParameterizedTest
    @CsvSource({
        "WARRANT, 42, WP-00000042",
        "WARRANT_OF_RESTITUTION, 7, WR-00000007",
        "WRIT, 12345678, HP-12345678",
        "WRIT_OF_RESTITUTION, 1, HR-00000001",
    })
    void shouldBuildTheNumberFromTheTypeAndTheNextSequenceValue(SelectEnforcementType type, long sequenceValue,
                                                                String expected) {
        when(enforcementOrderRepository.nextEnforcementNumberSequenceValue()).thenReturn(sequenceValue);

        assertThat(underTest.next(type)).isEqualTo(expected);
    }

    @Test
    void shouldNotPadASequenceValueLongerThanEightDigits() {
        when(enforcementOrderRepository.nextEnforcementNumberSequenceValue()).thenReturn(123456789L);

        assertThat(underTest.next(SelectEnforcementType.WARRANT)).isEqualTo("WP-123456789");
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldGiveEachTypeItsOwnPrefix(SelectEnforcementType type) {
        long typesWithTheSamePrefix = Arrays.stream(SelectEnforcementType.values())
            .filter(other -> EnforcementNumberGenerator.prefix(other).equals(EnforcementNumberGenerator.prefix(type)))
            .count();

        assertThat(typesWithTheSamePrefix).isEqualTo(1);
    }
}
