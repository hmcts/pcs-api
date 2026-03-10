package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.RawWarrantRestDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class VulnerableAdultsChildrenRestPageTest {

    @Mock
    private TextAreaValidationService service;

    @Mock
    private Event.EventBuilder<PCSCase, UserRole, State> eventBuilder;

    @Mock
    @SuppressWarnings("rawtypes")
    private FieldCollection.FieldCollectionBuilder fieldBuilder;

    private PageBuilder pageBuilder;

    private VulnerableAdultsChildrenRestPage pageUnderTest;

    @BeforeEach
    void setup() {
        pageBuilder = new PageBuilder(eventBuilder);
        pageUnderTest = new VulnerableAdultsChildrenRestPage(service);

        lenient().when(eventBuilder.fields()).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.page(anyString(), any(MidEvent.class))).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.pageLabel(AbstractVulnerableAdultsChildrenPage.PAGE_LABEL))
                .thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.showCondition(anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.label(anyString(), anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.complex(any())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.complex(any(), anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.mandatory(any())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.mandatory(any(), anyString())).thenReturn(fieldBuilder);
        lenient().when(fieldBuilder.done()).thenReturn(fieldBuilder);
    }

    @Test
    void shouldConfigurePageMetadataAndShowCondition() {
        assertThatCode(() -> pageUnderTest.addTo(pageBuilder))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldReturnExpectedShowCondition() {

        String condition = pageUnderTest.getVulnerablePeoplePresentShowCondition();

        assertThat(condition).contains("vulnerablePeoplePresentRest=\"YES\"")
            .contains("vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_ADULTS\"")
            .contains("vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_CHILDREN\"")
            .contains("vulnerableAdultsChildrenRest.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\"");
    }

    @Test
    void shouldReturnVulnerableReasonTextFromCase() {
        String expectedText = "Some vulnerability details";

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .rawWarrantRestDetails(RawWarrantRestDetails.builder()
                .vulnerableAdultsChildrenRest(VulnerableAdultsChildren.builder()
                    .vulnerableReasonText(expectedText)
                    .build())
                .build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(enforcementOrder)
            .build();

        String actual = pageUnderTest.getVulnerableReasonTextToValidate(caseData);

        assertThat(actual).isEqualTo(expectedText);
    }

    @Test
    void shouldReturnNullVulnerableReasonTextFromCaseIfNoDetailsFound() {

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
                .rawWarrantRestDetails(RawWarrantRestDetails.builder()
                        .vulnerableAdultsChildrenRest(null)
                        .build())
                .build();

        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(enforcementOrder)
                .build();

        String actual = pageUnderTest.getVulnerableReasonTextToValidate(caseData);

        assertThat(actual).isNull();
    }

    @Test
    void shouldReturnPageIdWithoutSuffix() {
        String pageId = pageUnderTest.getPageId();

        assertThat(pageId).isEqualTo("VulnerableAdultsChildrenRest");
    }
}
