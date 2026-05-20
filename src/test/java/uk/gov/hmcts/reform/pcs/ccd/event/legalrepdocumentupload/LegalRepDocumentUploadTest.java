package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepDocumentUploadTest extends BaseEventTest {

    @Mock
    private LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private LegalRepDocumentUpload legalRepDocumentUpload;

    @BeforeEach
    void setUp() {
        setEventUnderTest(legalRepDocumentUpload);
    }

    @Test
    void shouldConfigurePages() {
        PCSCase caseData = PCSCase.builder().build();

        callStartHandler(caseData);

        verify(legalRepDocumentUploadConfigurer).configurePages(any());
    }

    @Test
    void shouldBuildValidCategoriesWhenGenAppDatesExist() {
        LocalDateTime laterDate = LocalDateTime.of(2026, 4, 25, 10, 0);
        LocalDateTime earlierDate = LocalDateTime.of(2026, 4, 20, 10, 0);

        GenAppEntity earlierGenApp = GenAppEntity.builder()
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(earlierDate)
            .build();
        GenAppEntity laterGenApp = GenAppEntity.builder()
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(laterDate)
            .build();
        GenAppEntity generalGenApp = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder()
                .genApps(Set.of(earlierGenApp, laterGenApp, generalGenApp))
                .build());

        PCSCase result = callStartHandler(PCSCase.builder().build());

        assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
        DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
        assertThat(categories).isNotNull();
        assertThat(categories.getListItems()).hasSize(3);
        assertThat(categories.getListItems())
            .extracting(DynamicStringListElement::getCode)
            .containsExactlyInAnyOrder(
                DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name(),
                DocumentUploadCategory.GENERAL_APPLICATION.name(),
                DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());

        assertThat(categories.getListItems())
            .filteredOn(item -> DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name()
                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("Yes, the documents I'm uploading relate to the application to adjourn the "
                                 + "hearing - submitted on Saturday 25 Apr 2026");

        assertThat(categories.getListItems())
            .filteredOn(item -> DocumentUploadCategory.GENERAL_APPLICATION.name()
                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("Yes, the documents I'm uploading relate to an application submitted on "
                                 + "Saturday 25 Apr 2026");

        assertThat(result.getLegalRepDocumentUploadDetails().getShowExistingApplicationPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldKeepOnlyMainClaimOrCounterclaimWhenNoGenAppDatesAvailable() {
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder().build());

        PCSCase result = callStartHandler(PCSCase.builder().build());

        assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
        DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
        assertThat(categories).isNotNull();
        assertThat(categories.getListItems()).hasSize(1);
        assertThat(categories.getListItems().getFirst().getCode())
            .isEqualTo(DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());
    }
}
