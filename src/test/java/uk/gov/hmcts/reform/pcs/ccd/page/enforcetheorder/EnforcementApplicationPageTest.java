package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class EnforcementApplicationPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new EnforcementApplicationPage());
    }

    @Test
    void shouldSetFormattedDefendantNames_SingleDefendant() {
        // Given
        PCSCase caseData = createCaseWithDefendants(createDefendant("John", "Doe"));

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getFormattedDefendantNames())
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

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getFormattedDefendantNames())
                .isEqualTo("John Doe<br>\n"
                        + "Test Testing<br>\n"
                        + "Third Def<br>");
    }

    @Test
    void shouldSetFormattedDefendantNames_NoDefendants() {
        // Given
        PCSCase caseData = createCaseWithDefendants();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getEnforcementOrder().getFormattedDefendantNames()).isNull();
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
