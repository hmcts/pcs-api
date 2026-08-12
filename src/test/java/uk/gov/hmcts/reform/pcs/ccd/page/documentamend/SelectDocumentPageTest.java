package uk.gov.hmcts.reform.pcs.ccd.page.documentamend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.JsonProcessingException;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentListService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAssociationService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory.APPLICATIONS;

@ExtendWith(MockitoExtension.class)
class SelectDocumentPageTest extends BasePageTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private PartyService partyService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private DocumentAssociationService documentAssociationService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SelectDocumentPage(
            new DocumentSelectionService(
                pcsCaseService,
                new AddressFormatter()
            ),
            new DocumentAmendService(
                pcsCaseService,
                documentRepository,
                documentService,
                documentNameService,
                partyService,
                documentAssociationService,
                new CaseworkerDocumentListService(partyService)
            )
        ));
    }

    @Test
    void shouldPopulateFileDisplayNameForSelectedApplicationDocument() {
        UUID documentId = UUID.fromString("aae85c47-84ca-4531-a5a8-ba170cfb8742");
        DocumentEntity document = DocumentEntity.builder()
            .id(documentId)
            .fileName("Local test application.pdf")
            .categoryId(APPLICATIONS.getId())
            .build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder().addressLine1("15 Garden Drive").build())
            .applicationsDocuments(DynamicList.builder()
                .value(DynamicListElement.builder()
                    .code(documentId)
                    .build())
                .build())
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(APPLICATIONS)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        DocumentAmendDetails responseDetails = response.getData().getDocumentAmendDetails();
        assertThat(responseDetails.getSelectedDocumentId()).isEqualTo(documentId.toString());
        assertThat(responseDetails.getSelectedDocumentFileName()).isEqualTo("Local test application.pdf");
        assertThat(responseDetails.getSelectedDocumentBaseFileName()).isEqualTo("Local test application");
        assertThat(responseDetails.getAmendedFileName()).isEqualTo("Local test application");
    }

    @Test
    void shouldReturnFileDisplayNameInMidEventPayloadForSelectedApplicationDocument() throws JsonProcessingException {
        UUID documentId = UUID.fromString("aae85c47-84ca-4531-a5a8-ba170cfb8742");
        LocalDate issueDate = LocalDate.of(2026, 4, 16);
        DocumentEntity document = DocumentEntity.builder()
            .id(documentId)
            .fileName("Local test application.pdf")
            .categoryId(APPLICATIONS.getId())
            .issueDate(issueDate)
            .build();
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(PcsCaseEntity.builder()
            .documents(List.of(document))
            .build());
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder().addressLine1("15 Garden Drive").build())
            .applicationsDocuments(DynamicList.builder()
                .value(DynamicListElement.builder()
                    .code(documentId)
                    .build())
                .build())
            .documentAmendDetails(DocumentAmendDetails.builder()
                .selectedFolder(APPLICATIONS)
                .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        String payload = new JacksonConfiguration().getMapper().writeValueAsString(response);

        assertThat(payload)
            .contains("\"documentAmend_SelectedDocumentFileName\":\"Local test application.pdf\"")
            .contains("\"documentAmend_SelectedDocumentBaseFileName\":\"Local test application\"")
            .contains("\"documentAmend_SelectedDocumentIssueDate\":\"2026-04-16\"")
            .contains("\"documentAmend_IssueDate\":\"2026-04-16\"")
            .contains("\"documentAmend_AmendedFileName\":\"Local test application\"");
    }
}
