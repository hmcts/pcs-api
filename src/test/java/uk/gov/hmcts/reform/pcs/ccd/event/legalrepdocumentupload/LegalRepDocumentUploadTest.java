package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder()
                            .build());

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
            .withoutNotice(VerticalYesNo.YES)
            .build();

        GenAppEntity laterGenApp = GenAppEntity.builder()
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(laterDate)
            .withoutNotice(VerticalYesNo.YES)
            .build();

        GenAppEntity generalGenApp = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .withoutNotice(VerticalYesNo.YES)
            .build();

        GenAppEntity generalGenAppNoticeNo = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .withoutNotice(VerticalYesNo.NO)
            .build();

        GenAppEntity generalGenAppWithNullDate = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(null)
            .withoutNotice(VerticalYesNo.YES)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder()
                            .genApps(Set.of(
                                earlierGenApp,
                                laterGenApp,
                                generalGenApp,
                                generalGenAppNoticeNo,
                                generalGenAppWithNullDate))
                            .build());

        PCSCase result = callStartHandler(PCSCase.builder().build());

        assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();

        DynamicStringList categories =
            result.getLegalRepDocumentUploadDetails().getValidCategories();

        assertThat(categories).isNotNull();
        assertThat(categories.getListItems()).hasSize(4);

        assertThat(categories.getListItems())
            .extracting(DynamicStringListElement::getCode)
            .containsExactlyInAnyOrder(
                DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name(),
                DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name(),
                DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name(),
                DocumentUploadCategory.GENERAL_APPLICATION.name()
            );

        assertThat(categories.getListItems())
            .filteredOn(item ->
                            DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name()
                                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactlyInAnyOrder(
                "Yes, the documents I’m uploading relate to the application to adjourn the "
                    + "hearing - submitted on Monday 20 Apr 2026",
                "Yes, the documents I’m uploading relate to the application to adjourn the "
                    + "hearing - submitted on Saturday 25 Apr 2026"
            );

        assertThat(categories.getListItems())
            .filteredOn(item ->
                            DocumentUploadCategory.GENERAL_APPLICATION.name()
                                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly(
                "Yes, the documents I’m uploading relate to an application submitted on "
                    + "Saturday 25 Apr 2026"
            );

        assertThat(categories.getListItems())
            .filteredOn(item ->
                            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name()
                                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly(
                "No, the documents I’m uploading relate to the main claim or counterclaim"
            );

        assertThat(result.getLegalRepDocumentUploadDetails().getShowExistingApplicationPage())
            .isEqualTo(YesOrNo.YES);
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

    @Test
    void shouldReturnNullForLatestGenAppDateWhenGenAppsIsNull() {
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder()
                .genApps(null)
                .build());

        PCSCase result = callStartHandler(PCSCase.builder().build());

        assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
        DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
        assertThat(categories).isNotNull();
        assertThat(categories.getListItems()).hasSize(1);
        assertThat(categories.getListItems().getFirst().getCode())
            .isEqualTo(DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());
    }

    @Test
    void shouldReturnNullForUnmappedCategoryType() {
        assertThat(legalRepDocumentUpload.mapCategoryToGenAppType(
            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM))
            .isNull();
    }

    @Test
    void shouldReturnNullForLatestGenAppDateWhenCategoryIsUnmapped() {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .genApps(Set.of())
            .build();

        assertThat(legalRepDocumentUpload.findGenAppDatesForCategory(
            pcsCaseEntity,
            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM))
            .isEmpty();
    }
}
