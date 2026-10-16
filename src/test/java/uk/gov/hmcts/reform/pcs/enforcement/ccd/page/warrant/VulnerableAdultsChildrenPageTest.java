package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrant;

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
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.VulnerableAdultsChildren;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.AbstractVulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class VulnerableAdultsChildrenPageTest {

    @Mock
    private TextAreaValidationService service;

    @Mock
    private Event.EventBuilder<PCSCase, UserRole, State> eventBuilder;

    @Mock
    @SuppressWarnings("rawtypes")
    private FieldCollection.FieldCollectionBuilder fieldBuilder;

    private PageBuilder pageBuilder;

    private VulnerableAdultsChildrenPage pageUnderTest;

    @BeforeEach
    void setup() {
        pageBuilder = new PageBuilder(eventBuilder);
        pageUnderTest = new VulnerableAdultsChildrenPage(service);

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
        assertThatCode(() -> pageUnderTest.addTo(pageBuilder)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnVulnerableReasonTextFromCase() {
        String expectedText = "Some vulnerability details";

        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .rawWarrantDetails(RawWarrantDetails.builder()
                .vulnerableAdultsChildren(VulnerableAdultsChildren.builder()
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
                .rawWarrantDetails(RawWarrantDetails.builder()
                        .vulnerableAdultsChildren(null)
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
        String pageId = pageUnderTest.getPageKey();

        assertThat(pageId).isEqualTo("vulnerableAdultsChildren");
    }
}
