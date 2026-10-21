package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.updateddocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.caseworker.updatedocument.DocumentUpdated;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;

@ExtendWith(MockitoExtension.class)
class DocumentUpdatedTest extends BaseEventTest {

    @Mock
    private DocumentCategoryUpdateService documentCategoryUpdateService;

    @BeforeEach
    void setUp() {
        DocumentUpdated underTest = new DocumentUpdated(documentCategoryUpdateService);

        setEventUnderTest(underTest);
    }

    @Test
    void shouldRegisterSystemEventWithOnlyInternalAccess() {
        assertThat(configuredEvent.getId()).isEqualTo("DocumentUpdated");
        assertThat(getEventFieldIds()).isEmpty();
        assertThat(configuredEvent.getGrants().keySet()).containsExactlyInAnyOrder(DOCUMENT_MOVE_ROLES);
        assertThat(configuredEvent.getGrants().values()).containsOnly(Permission.R);
    }

    @Test
    void shouldPassCcdDocumentToPersist() {
        var documents = List.of(ListValue.<Document>builder()
                .id("document-1").value(Document.builder().categoryId("evidence").build()).build());

        callSubmitHandler(PCSCase.builder().allDocuments(documents).build());

        verify(documentCategoryUpdateService).updateCategories(TEST_CASE_REFERENCE, documents);
    }
}
