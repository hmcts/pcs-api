package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        PCSCase caseData = createCaseWithDefendants();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getFormattedDefendantNames()).isNull();
    }

    @Test
    void shouldNotAddErrorWhenWarrantSelected() {
        // Given
        PCSCase caseData = createCaseWithEnforcementType(null, SelectEnforcementType.WARRANT);

        // When
        var response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void shouldNotAddErrorWhenWritSelectedAndClaimNotTransferred() {
        // Given
        PCSCase caseData = createCaseWithEnforcementType(
            WritDetails.builder()
                .hasClaimTransferredToHighCourt(YesOrNo.NO)
                .wasGeneralApplicationToTransferToHighCourtSuccessful(null)
                .build(),
            SelectEnforcementType.WRIT
        );

        // When
        var response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void shouldNotAddErrorWhenWritSelectedAndClaimTransferredAndGeneralApplicationSuccessful() {
        // Given
        PCSCase caseData = createCaseWithEnforcementType(
            WritDetails.builder()
                .hasClaimTransferredToHighCourt(YesOrNo.YES)
                .wasGeneralApplicationToTransferToHighCourtSuccessful(YesOrNo.YES)
                .build(),
            SelectEnforcementType.WRIT
        );

        // When
        var response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
    }

    @Test
    void shouldAddErrorWhenWritSelectedAndClaimTransferredButGeneralApplicationUnsuccessful() {
        // Given
        PCSCase caseData = createCaseWithEnforcementType(
            WritDetails.builder()
                .hasClaimTransferredToHighCourt(YesOrNo.YES)
                .wasGeneralApplicationToTransferToHighCourtSuccessful(YesOrNo.NO)
                .build(),
            SelectEnforcementType.WRIT
        );

        // When
        var response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors())
            .singleElement()
            .isEqualTo(
                "You cannot continue with this application because your "
                    + "application to transfer to the High Court was unsuccessful"
            );
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

    private PCSCase createCaseWithEnforcementType(WritDetails writDetails, SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .selectEnforcementType(enforcementType)
            .writDetails(writDetails)
            .build();

        return PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();
    }

    private Party createDefendant(String firstName, String lastName) {
        return Party.builder()
                .firstName(firstName)
                .lastName(lastName).build();
    }
}
