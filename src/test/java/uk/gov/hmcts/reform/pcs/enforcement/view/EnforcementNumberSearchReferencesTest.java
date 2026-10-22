package uk.gov.hmcts.reform.pcs.enforcement.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementNumberSearchReferencesTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @InjectMocks
    private EnforcementNumberSearchReferences underTest;

    @Test
    void shouldGiveEveryWarrantAndWritNumberOnTheCase() {
        when(enforcementOrderRepository.findEnforcementNumbersByCaseReference(CASE_REFERENCE))
            .thenReturn(List.of("WP-00000001", "HP-00000002"));

        assertThat(underTest.otherCaseReferences(CASE_REFERENCE)).containsExactly("WP-00000001", "HP-00000002");
    }

    @Test
    void shouldGiveNothingWhenNoNumberHasBeenGiven() {
        when(enforcementOrderRepository.findEnforcementNumbersByCaseReference(CASE_REFERENCE))
            .thenReturn(List.of());

        assertThat(underTest.otherCaseReferences(CASE_REFERENCE)).isEmpty();
    }
}
