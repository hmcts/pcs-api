package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.model.PartyDocumentDto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class CitizenDocumentsService {

    public List<PartyDocumentDto> buildCitizenDocuments(PCSCase pcsCase) {
        if (pcsCase.getCitizenDocuments() == null || pcsCase.getCitizenDocuments().isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Building citizen documents, count: {}", pcsCase.getCitizenDocuments().size());

        return pcsCase.getCitizenDocuments().stream()
            .map(ListValue::getValue)
            .filter(doc -> doc != null && doc.getDocument() != null)
            .map(this::mapToPartyDocumentDto)
            .collect(Collectors.toList());
    }

    private PartyDocumentDto mapToPartyDocumentDto(AdditionalDocument additionalDocument) {
        return PartyDocumentDto.builder()
            .documentType(additionalDocument.getDocumentType())
            .document(additionalDocument.getDocument())
            .description(additionalDocument.getDescription())
            .build();
    }
}
