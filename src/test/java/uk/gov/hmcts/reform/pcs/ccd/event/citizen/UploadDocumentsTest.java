package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadDocumentsTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        UploadDocuments underTest = new UploadDocuments(pcsCaseService, partyService,
                                                        securityContextService, documentService);
        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Submit event tests")
    class SubmitTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @Test
        void shouldPersistUploadedDocumentsForCurrentParty() {
            // Given
            UploadedDocument uploaded = UploadedDocument.builder()
                .document(Document.builder()
                    .url("url-1").filename("file-1.pdf").binaryUrl("bin-1").build())
                .build();

            List<ListValue<UploadedDocument>> uploadedDocs = List.of(
                ListValue.<UploadedDocument>builder().id("1").value(uploaded).build()
            );

            PCSCase caseData = PCSCase.builder()
                .uploadedAdditionalDocuments(uploadedDocs)
                .build();

            PartyEntity currentParty = stubCurrentUserParty();

            // When
            callSubmitHandler(caseData);

            // Then
            verify(documentService).createAdditionalDocumentsForParty(uploadedDocs, pcsCaseEntity, currentParty);
        }

        @Test
        void shouldDelegateEvenWhenNoDocumentsSent() {
            // Given
            PCSCase caseData = PCSCase.builder().build();
            PartyEntity currentParty = stubCurrentUserParty();

            // When
            callSubmitHandler(caseData);

            // Then — service handles null/empty; event passes through
            verify(documentService).createAdditionalDocumentsForParty(null, pcsCaseEntity, currentParty);
        }

        private PartyEntity stubCurrentUserParty() {
            PartyEntity currentUserParty = mock(PartyEntity.class);
            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE))
                .willReturn(currentUserParty);
            return currentUserParty;
        }
    }
}
