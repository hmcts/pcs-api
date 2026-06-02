package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class GenAppsView {

    private final ModelMapper modelMapper;
    private final SecurityContextService securityContextService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        UUID currentUserId = securityContextService.getCurrentUserId();

        List<ListValue<GeneralApplication>> genApps = pcsCaseEntity.getGenApps().stream()
            .sorted(Comparator.comparing(GenAppEntity::getApplicationSubmittedDate).reversed())
            .filter(genAppEntity -> isVisibleToUser(genAppEntity, currentUserId))
            .map(this::createListValue)
            .toList();

        pcsCase.setGenApps(genApps);
    }

    private ListValue<GeneralApplication> createListValue(GenAppEntity genAppEntity) {
        Party party = mapToSimpleParty(genAppEntity);

        GeneralApplication generalApplication = GeneralApplication.builder()
            .applicationType(genAppEntity.getType())
            .party(party)
            .submittedOn(genAppEntity.getApplicationSubmittedDate())
            .submissionDocument(getSubmissionDocument(genAppEntity))
            .supportingDocuments(getSupportingDocuments(genAppEntity))
            .build();

        return new ListValue<>(genAppEntity.getId().toString(), generalApplication);
    }

    private Party mapToSimpleParty(GenAppEntity genAppEntity) {
        return Optional.ofNullable(genAppEntity.getParty())
            .map(partyEntity -> {
                UUID idamId = partyEntity.getIdamId();

                return Party.builder()
                    .id(partyEntity.getId().toString())
                    .idamId(idamId != null ? idamId.toString() : null)
                    .firstName(partyEntity.getFirstName())
                    .lastName(partyEntity.getLastName())
                    .build();
            })
            .orElse(null);
    }

    private DocumentWithId getSubmissionDocument(GenAppEntity genAppEntity) {
        return Optional.ofNullable(genAppEntity.getSubmissionDocument())
            .map(documentEntity -> DocumentWithId.builder()
                .id(documentEntity.getId().toString())
                .document(modelMapper.map(documentEntity, Document.class))
                .build()
            )
            .orElse(null);
    }

    private List<ListValue<Document>> getSupportingDocuments(GenAppEntity genAppEntity) {
        return Optional.ofNullable(genAppEntity.getDocuments())
            .filter(documents -> !documents.isEmpty())
            .map(documents -> documents.stream()
                .map(this::toListValue)
                .collect(Collectors.toList()))
            .orElse(null);
    }

    private ListValue<Document> toListValue(DocumentEntity documentEntity) {
        return ListValue.<Document>builder()
            .id(documentEntity.getId().toString())
            .value(modelMapper.map(documentEntity, Document.class))
            .build();
    }

    private boolean isVisibleToUser(GenAppEntity genAppEntity, UUID userId) {
        return genAppEntity.getWithoutNotice() != VerticalYesNo.YES
            || userId.equals(genAppEntity.getParty().getIdamId());
    }

}
