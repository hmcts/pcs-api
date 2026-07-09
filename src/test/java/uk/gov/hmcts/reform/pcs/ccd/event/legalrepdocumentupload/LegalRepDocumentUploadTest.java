package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.DocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Mock
    private DocumentService documentService;

    @Mock
    private PartyService partyService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private GenAppVisibilityService genAppVisibilityService;

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

        GenAppEntity generalGenAppNullNotice = GenAppEntity.builder()
            .type(GenAppType.SOMETHING_ELSE)
            .applicationSubmittedDate(laterDate)
            .withoutNotice(null)
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
                                generalGenAppNullNotice,
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

    @Test
    void shouldUploadLegalRepDocumentCorrectly() {
        // Given
        String description = "test description";
        UUID selectedId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Document document = Document.builder()
            .filename("test filename")
            .url("test url")
            .binaryUrl("test binary url")
            .build();

        LegalRepDocument legalRepDocument = LegalRepDocument.builder()
            .document(document)
            .legalRepDocumentType(LegalRepDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .description(description)
            .build();

        DocumentUploadDetails documentUploadDetails = DocumentUploadDetails.builder()
            .selectedRelatedApplicationId(selectedId.toString())
            .build();

        List<LegalRepDocument> legalRepDocList = List.of(legalRepDocument);

        PCSCase pcsCase = PCSCase.builder()
            .documentUploadDetails(documentUploadDetails)
            .build();

        GenAppEntity selectedGenApp = mock(GenAppEntity.class);
        PartyEntity currentUserParty = mock(PartyEntity.class);

        when(documentService.createLegalRepDocuments(pcsCase)).thenReturn(legalRepDocList);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        when(selectedGenApp.getId()).thenReturn(selectedId);
        when(pcsCaseEntity.getGenApps()).thenReturn(Set.of(selectedGenApp));
        List<GenAppEntity> mockGenAppList = List.of(selectedGenApp);
        when(genAppVisibilityService.getVisibleGenAppsToUser(Set.of(selectedGenApp), currentUserId))
            .thenReturn(mockGenAppList);

        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(currentUserParty);

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(documentService, times(1))
            .createDocumentEntitiesFromLegalRepDocuments(legalRepDocList,pcsCaseEntity,currentUserParty,selectedGenApp);
    }

    @Test
    void shouldReturnErrorWhenGetDocumentIsNull() {

        LegalRepDocument legalRepDocument = LegalRepDocument.builder()
            .description("test description")
            .document(null)
            .legalRepDocumentType(LegalRepDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .build();

        List<LegalRepDocument> legalRepDocList = List.of(legalRepDocument);

        PCSCase pcsCase = PCSCase.builder()
            .build();

        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(documentService.createLegalRepDocuments(pcsCase)).thenReturn(legalRepDocList);

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
            .legalRepDocumentType(LegalRepDocumentType.PHOTOGRAPHIC_EVIDENCE)
            .build();

        List<LegalRepDocument> legalRepDocList = Stream.of(nullLegalRepDocument,
                                                           validLegalRepDocument).toList();

        PCSCase pcsCase = PCSCase.builder()
            .build();

        when(documentService.createLegalRepDocuments(pcsCase)).thenReturn(legalRepDocList);

        SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

        assertThat(submitResponse.getErrors().contains("Your files were not submitted. Try again."));
    }
}
