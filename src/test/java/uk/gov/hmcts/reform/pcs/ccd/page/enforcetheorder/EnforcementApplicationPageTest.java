package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrantofrestitution.WarrantOfRestitutionMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementOrder;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementOrderWithSpecifiedType;

@ExtendWith(MockitoExtension.class)
class EnforcementApplicationPageTest extends BasePageTest {

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private WarrantOfRestitutionMapper warrantOfRestitutionMapper;
    @InjectMocks
    private EnforcementApplicationPage enforcementApplicationPage;

    @BeforeEach
    void setUp() {
        setPageUnderTest(enforcementApplicationPage);
    }

    @Test
    void shouldSetFormattedDefendantNames_SingleDefendant() {
        // Given
        PCSCase caseData = createCaseWithDefendants(createDefendant("John", "Doe"));
        caseData.setEnforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT));

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getFormattedDefendantNames())
                .isEqualTo("John Doe<br>");
    }

    @Test
    void shouldSetFormattedDefendantNames_MultipleDefendant() {
        // Given
        PCSCase caseData = createCaseWithDefendants(
                createDefendant("John", "Doe"),
                createDefendant("Test", "Testing"),
                createDefendant("Third", "Def")
        );
        caseData.setEnforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT));

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getFormattedDefendantNames())
                .isEqualTo("John Doe<br>\n"
                        + "Test Testing<br>\n"
                        + "Third Def<br>");
    }

    @Test
    void shouldSetFormattedDefendantNames_NoDefendants() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT))
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getFormattedDefendantNames()).isNull();
    }

    @Test
    void shouldPrepopulateWarrantRestDetailsIfSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT_OF_RESTITUTION))
                .build();

        EnforcementOrder warrantEnforcementOrder = buildEnforcementOrder();

        when(enforcementOrderService.retrieveEnforcementOrder(TEST_CASE_REFERENCE, WARRANT))
                .thenReturn(warrantEnforcementOrder);
        // When
        callMidEventHandler(caseData);

        // Then
        verify(warrantOfRestitutionMapper).prePopulateFieldsFromWarrantDetails(warrantEnforcementOrder,
                caseData.getEnforcementOrder());
    }

    @Test
    void shouldNotPrepopulateWarrantRestDetailsIfNoWarrantOrder() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(buildEnforcementOrderWithSpecifiedType(WARRANT_OF_RESTITUTION))
                .build();

        when(enforcementOrderService.retrieveEnforcementOrder(TEST_CASE_REFERENCE, WARRANT))
                .thenReturn(null);
        // When
        callMidEventHandler(caseData);

        // Then
        verify(warrantOfRestitutionMapper, never()).prePopulateFieldsFromWarrantDetails(any(), any());
    }

    private PCSCase createCaseWithDefendants(Party... defendants) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        List<ListValue<Party>> defendantList = new ArrayList<>();

        for (Party defendant : defendants) {
            defendantList.add(ListValue.<Party>builder().value(defendant).build());
        }

        PCSCase caseData = PCSCase.builder().enforcementOrder(enforcementOrder).build();
        caseData.setAllDefendants(defendantList);
        return caseData;
    }

    private Party createDefendant(String firstName, String lastName) {
        return Party.builder()
                .firstName(firstName)
                .lastName(lastName).build();
    }
}
