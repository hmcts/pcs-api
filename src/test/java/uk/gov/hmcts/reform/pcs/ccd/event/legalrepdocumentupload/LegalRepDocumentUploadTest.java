package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;

import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepDocumentUploadTest extends BaseEventTest {

    @Mock
    private LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;

    @Mock
    private PcsCaseEntity pcsCaseEntity;

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private LegalRepDocumentUpload legalRepDocumentUpload;

    @Captor
    private ArgumentCaptor<List<DocumentEntity>> listCaptor;

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
            .build();
        GenAppEntity laterGenApp = GenAppEntity.builder()
            .type(GenAppType.ADJOURN)
            .applicationSubmittedDate(laterDate)
            .build();
        GenAppEntity generalGenApp = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .build();
        GenAppEntity generalGenAppWithNullDate = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(null)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE))
            .thenReturn(PcsCaseEntity.builder()
                .genApps(Set.of(earlierGenApp, laterGenApp, generalGenApp,  generalGenAppWithNullDate))
                .build());

        PCSCase result = callStartHandler(PCSCase.builder().build());

        assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
        DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
        assertThat(categories).isNotNull();
        assertThat(categories.getListItems()).hasSize(3);
        assertThat(categories.getListItems())
            .extracting(DynamicStringListElement::getCode)
            .containsExactly(
                DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name(),
                DocumentUploadCategory.GENERAL_APPLICATION.name(),
                DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());

        assertThat(categories.getListItems())
            .filteredOn(item -> DocumentUploadCategory.ADJOURN_HEARING_APPLICATION.name()
                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("Yes, the documents I’m uploading relate to the application to adjourn the "
                                 + "hearing - submitted on Saturday 25 Apr 2026");

        assertThat(categories.getListItems())
            .filteredOn(item -> DocumentUploadCategory.GENERAL_APPLICATION.name()
                .equals(item.getCode()))
            .extracting(DynamicStringListElement::getLabel)
            .containsExactly("Yes, the documents I’m uploading relate to an application submitted on "
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

        assertThat(legalRepDocumentUpload.findLatestGenAppDateForCategory(
            pcsCaseEntity,
            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM))
            .isNull();
    }

    @Test
    void shouldUploadLegalRepDocumentCorrectly() {
        // Given
        String description = "test description";

        Document document = Document.builder()
            .filename("test filename")
            .url("test url")
            .binaryUrl("test binary url")
            .build();

        LegalRepDocument legalRepDocument = LegalRepDocument.builder()
            .document(document)
            .documentType(EvidenceDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .description(description)
            .build();

        List<ListValue<LegalRepDocument>> legalRepDocList = List.of(ListValue.<LegalRepDocument>builder()
                                                                        .value(legalRepDocument).build());

        LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = LegalRepDocumentUploadDetails.builder()
            .legalRepDocuments(legalRepDocList).build();

        PCSCase pcsCase = PCSCase.builder()
            .legalRepDocumentUploadDetails(legalRepDocumentUploadDetails)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseEntity, times(1)).addDocuments(listCaptor.capture());

        List<DocumentEntity> capturedDocumentList = listCaptor.getValue();
        assertThat(capturedDocumentList).hasSize(1);

        DocumentEntity documentEntity = capturedDocumentList.get(0);

        assertThat(documentEntity.getUrl()).isEqualTo("test url");
        assertThat(documentEntity.getFileName()).isEqualTo("test filename");
        assertThat(documentEntity.getDescription()).isEqualTo(description);
    }

    @Test
    void shouldReturnErrorWhenGetDocumentIsNull() {

        LegalRepDocument legalRepDocument = LegalRepDocument.builder()
            .description("test description")
            .document(null)
            .documentType(EvidenceDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .build();

        List<ListValue<LegalRepDocument>> legalRepDocList = List.of(ListValue.<LegalRepDocument>builder()
                                                                        .value(legalRepDocument).build());

        LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = LegalRepDocumentUploadDetails.builder()
            .legalRepDocuments(legalRepDocList)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .legalRepDocumentUploadDetails(legalRepDocumentUploadDetails)
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        assertThat(submitResponse.getErrors().contains("Your files were not submitted. Try again."));
    }


    @Test
    void shouldReturnErrorWhenLegalRepDocumentIsNull() {


        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        LegalRepDocument nullLegalRepDocument = null;
        LegalRepDocument validLegalRepDocument = LegalRepDocument.builder()
            .description("test description")
            .document(new Document())
            .documentType(EvidenceDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .build();

        List<ListValue<LegalRepDocument>> legalRepDocList = List.of(
            ListValue.<LegalRepDocument>builder().value(nullLegalRepDocument).build(),
            ListValue.<LegalRepDocument>builder().value(validLegalRepDocument).build()
        );

        LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = LegalRepDocumentUploadDetails.builder()
            .legalRepDocuments(legalRepDocList)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .legalRepDocumentUploadDetails(legalRepDocumentUploadDetails)
            .build();

        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        assertThat(submitResponse.getErrors().contains("Your files were not submitted. Try again."));
    }
}
