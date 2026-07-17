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
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Mock
    private GenAppVisibilityService genAppVisibilityService;

    @Mock
    private SecurityContextService securityContextService;

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

        UUID earlierAdjournId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID laterAdjournId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID generalId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        GenAppEntity earlierAdjournApp = GenAppEntity.builder()
            .id(earlierAdjournId)
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(earlierDate)
            .build();

        GenAppEntity laterAdjournApp = GenAppEntity.builder()
            .id(laterAdjournId)
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(laterDate)
            .build();

        GenAppEntity generalApp = GenAppEntity.builder()
            .id(generalId)
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .build();

        GenAppEntity generalAppWithNullDate = GenAppEntity.builder()
            .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(null)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder().build());

        when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
            .thenReturn(List.of(earlierAdjournApp, laterAdjournApp, generalApp, generalAppWithNullDate));

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
                earlierAdjournId.toString(),
                laterAdjournId.toString(),
                generalId.toString()
            );

        assertThat(categories.getListItems())
            .filteredOn(item -> item.getLabel().contains("adjourn the hearing"))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactlyInAnyOrder(
                "Yes, the documents I’m uploading relate to the application to adjourn the "
                    + "hearing - submitted on Monday 20 Apr 2026",
                "Yes, the documents I’m uploading relate to the application to adjourn the "
                    + "hearing - submitted on Saturday 25 Apr 2026"
            );

        assertThat(categories.getListItems())
            .filteredOn(item -> item.getLabel().equals(
                "Yes, the documents I’m uploading relate to an application submitted on "
                    + "Saturday 25 Apr 2026"))
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(generalId.toString());

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
}
