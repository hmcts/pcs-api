package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Component
@AllArgsConstructor
public class GenAppsView {

    private final ModelMapper modelMapper;
    private final SecurityContextService securityContextService;
    private final GenAppVisibilityService genAppVisibilityService;
    private final DocumentRepository documentRepository;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        UUID currentUserId = securityContextService.getCurrentUserId();

        List<GenAppEntity> visibleGenApps = pcsCaseEntity.getGenApps().stream()
            .sorted(Comparator.comparing(GenAppEntity::getApplicationSubmittedDate).reversed())
            .filter(genAppEntity -> genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, currentUserId))
            .toList();
        Map<UUID, List<DocumentEntity>> documentsByGenAppId = getDocumentsByGenAppId(visibleGenApps);

        List<ListValue<GeneralApplication>> genApps = visibleGenApps.stream()
            .map(genApp -> createListValue(genApp, documentsByGenAppId))
            .toList();

        pcsCase.setGenApps(genApps);
    }

    private ListValue<GeneralApplication> createListValue(GenAppEntity genAppEntity,
                                                          Map<UUID, List<DocumentEntity>> documentsByGenAppId) {
        Party party = mapToSimpleParty(genAppEntity);

        GeneralApplication generalApplication = GeneralApplication.builder()
            .applicationType(genAppEntity.getType())
            .party(party)
            .submittedOn(genAppEntity.getApplicationSubmittedDate())
            .submissionDocument(getSubmissionDocument(genAppEntity))
            .supportingDocuments(createSupportingDocumentList(
                documentsByGenAppId.getOrDefault(genAppEntity.getId(), List.of())))
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

    private Map<UUID, List<DocumentEntity>> getDocumentsByGenAppId(List<GenAppEntity> genApps) {
        Set<UUID> genAppIds = genApps.stream()
            .map(GenAppEntity::getId)
            .collect(toSet());

        if (genAppIds.isEmpty()) {
            return Map.of();
        }

        return documentRepository.findAllByGeneralApplicationIds(genAppIds).stream()
            .collect(groupingBy(document -> document.getGeneralApplication().getId()));
    }

    private List<ListValue<Document>> createSupportingDocumentList(List<DocumentEntity> documents) {
        return documents.stream()
            .map(documentEntity -> {
                Document document = modelMapper.map(documentEntity, Document.class);
                return ListValue.<Document>builder()
                    .id(documentEntity.getId().toString())
                    .value(document)
                    .build();
            })
            .toList();
    }

}
