package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.model.PartyDocumentDto;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling party documents mapping and business logic.
 * Maps from CCD domain model to service DTOs.
 */
@Service
@AllArgsConstructor
public class PartyDocumentsService {

    /**
     * Builds party documents from PCSCase additionalDocuments field.
     * Maps from CCD domain model to service DTO.
     */
    public List<PartyDocumentDto> buildPartyDocuments(PCSCase pcsCase) {
        return ListValueUtils.unwrapListItems(pcsCase.getAdditionalDocuments())
                .stream()
                .map(this::mapAdditionalDocumentToPartyDocument)
                .collect(Collectors.toList());
    }

    /**
     * Builds citizen documents from PCSCase citizenDocuments field.
     * Maps from CCD domain model to service DTO.
     */
    public List<PartyDocumentDto> buildCitizenDocuments(PCSCase pcsCase) {
        if (pcsCase.getCitizenDocuments() == null) {
            return Collections.emptyList();
        }

        return ListValueUtils.unwrapListItems(pcsCase.getCitizenDocuments())
            .stream()
            .map(this::mapAdditionalDocumentToPartyDocument)
            .collect(Collectors.toList());
    }

    /**
     * Maps AdditionalDocument to PartyDocumentDto.
     * Only includes essential fields: description, documentType, document.
     */
    private PartyDocumentDto mapAdditionalDocumentToPartyDocument(
            AdditionalDocument additionalDocument) {
        return PartyDocumentDto.builder()
                .description(additionalDocument.getDescription())
                .documentType(additionalDocument.getDocumentType())
                .document(additionalDocument.getDocument())
                .build();
    }
}
