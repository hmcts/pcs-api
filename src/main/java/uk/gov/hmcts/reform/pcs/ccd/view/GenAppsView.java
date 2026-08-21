package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;

import java.util.Comparator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class GenAppsView {

    private final UserRoleService userRoleService;
    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, String organisationId) {
        Collection<GenAppEntity> genAppEntities = pcsCaseEntity.getGenApps();
        if (genAppEntities == null || genAppEntities.isEmpty()) {
            pcsCase.setGenApps(List.of());
            return;
        }

        UserRoles userRoles =
            userRoleService.getCurrentUserCaseRoles(pcsCaseEntity.getCaseReference());

        List<ListValue<GeneralApplication>> genApps = genAppEntities.stream()
            .sorted(Comparator.comparing(GenAppEntity::getApplicationSubmittedDate).reversed())
            .filter(genAppEntity -> genAppVisibilityService.isGenAppVisibleToUser(
                genAppEntity,
                organisationId,
                userRoles.roles()
            ))
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
            .state(genAppEntity.getState())
            .submissionDocument(getSubmissionDocument(genAppEntity))
            .supportingDocuments(createSupportingDocumentList(genAppEntity))
            .rank(genAppEntity.getRank())
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
                .document(mapDocument(documentEntity))
                .build())
            .orElse(null);
    }

    private List<ListValue<Document>> createSupportingDocumentList(GenAppEntity genAppEntity) {
        Set<String> seenDocumentReferences = new HashSet<>();
        addDocumentReferences(genAppEntity.getSubmissionDocument(), seenDocumentReferences);

        return genAppEntity.getDocuments().stream()
            .filter(documentEntity -> isNewDocument(documentEntity, seenDocumentReferences))
            .map(documentEntity -> {
                return ListValue.<Document>builder()
                    .id(documentEntity.getId().toString())
                    .value(mapDocument(documentEntity))
                    .build();
            })
            .toList();
    }

    private Document mapDocument(DocumentEntity documentEntity) {
        return Document.builder()
            .filename(documentEntity.getFileName())
            .url(documentEntity.getUrl())
            .binaryUrl(documentEntity.getBinaryUrl())
            .categoryId(documentEntity.getCategoryId())
            .build();
    }

    private boolean isNewDocument(DocumentEntity documentEntity, Set<String> seenDocumentReferences) {
        Set<String> documentReferences = documentReferences(documentEntity).collect(Collectors.toSet());

        if (documentReferences.isEmpty()) {
            return true;
        }

        boolean alreadySeen = documentReferences.stream().anyMatch(seenDocumentReferences::contains);
        seenDocumentReferences.addAll(documentReferences);

        return !alreadySeen;
    }

    private void addDocumentReferences(DocumentEntity documentEntity, Set<String> documentReferences) {
        documentReferences(documentEntity).forEach(documentReferences::add);
    }

    private Stream<String> documentReferences(DocumentEntity documentEntity) {
        if (documentEntity == null) {
            return Stream.empty();
        }

        String documentEntityId = Optional.ofNullable(documentEntity.getId())
            .map(UUID::toString)
            .orElse(null);

        return Stream.of(documentEntityId, documentEntity.getUrl(), documentEntity.getBinaryUrl())
            .filter(Objects::nonNull);
    }

}
