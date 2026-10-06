package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TestCaseDocumentService {

    private static final UUID PLACEHOLDER_DOCUMENT_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String DOCUMENT_URL =
        "http://dm-store/documents/" + PLACEHOLDER_DOCUMENT_ID;

    private static final String BINARY_URL =
        DOCUMENT_URL + "/binary";

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;

    @Transactional
    public void addPossessionOrderPlaceholder(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        DocumentEntity possessionOrder = DocumentEntity.builder()
            .documentId(PLACEHOLDER_DOCUMENT_ID)
            .fileName("Possession order.pdf")
            .url(DOCUMENT_URL)
            .binaryUrl(BINARY_URL)
            .type(DocumentType.POSSESSION_ORDER)
            .categoryId(
                CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS.getId()
            )
            .contentType("application/pdf")
            .build();

        pcsCaseEntity.addDocument(possessionOrder);

        documentRepository.save(possessionOrder);
    }
}
