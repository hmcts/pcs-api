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
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class GenAppsView {

    private final ModelMapper modelMapper;
    private final OrganisationService organisationService;
    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        String orgId = organisationService.getOrganisationIdForCurrentUser();

        List<ListValue<GeneralApplication>> genApps = pcsCaseEntity.getGenApps().stream()
            .sorted(Comparator.comparing(GenAppEntity::getApplicationSubmittedDate).reversed())
            .filter(genAppEntity -> genAppVisibilityService.isGenAppVisibleToUser(genAppEntity, orgId))
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
            .supportingDocuments(createSupportingDocumentList(genAppEntity))
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

    private List<ListValue<Document>> createSupportingDocumentList(GenAppEntity genAppEntity) {
        return genAppEntity.getDocuments().stream()
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
