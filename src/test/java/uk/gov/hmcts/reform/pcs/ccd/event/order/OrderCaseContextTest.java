package uk.gov.hmcts.reform.pcs.ccd.event.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCaseContextTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private AddressMapper addressMapper;
    @Mock
    private PartyService partyService;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    private OrderCaseContext underTest;

    @BeforeEach
    void setUp() {
        underTest = new OrderCaseContext(addressMapper, partyService, caseNameFormatter);
    }

    @Test
    void shouldSetCaseNameForOrderStartCaseContext() {
        // Given
        String expectedCaseName = "some case name";
        when(caseNameFormatter.formatCaseName(anyList(), anyList())).thenReturn(expectedCaseName);

        ClaimEntity mainClaim = ClaimEntity.builder()
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .claims(List.of(mainClaim))
            .build();

        // When
        OrderStart.CaseContext orderStartCaseContext = underTest.of(pcsCaseEntity);

        // Then
        assertThat(orderStartCaseContext.caseName()).isEqualTo(expectedCaseName);
    }
}
