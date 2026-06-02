package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution.EvidenceDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;

import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepDocumentUpload;

@Component
@AllArgsConstructor
public class LegalRepDocumentUpload implements CCDConfig<PCSCase, State, UserRole> {

    private final LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;
    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(legalRepDocumentUpload.name(), this::submit, this::start)
                .forAllStates()
                .name("Upload additional documents")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary()
                .endButtonLabel("${legalRepUploadDocumentDetails.endButtonLabel}");
        legalRepDocumentUploadConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        if (caseData.getLegalRepDocumentUploadDetails() == null) {
            caseData.setLegalRepDocumentUploadDetails(
                new LegalRepDocumentUploadDetails());
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        List<DynamicStringListElement> validCategoryItems =
            Arrays.stream(DocumentUploadCategory.values())
                .map(category -> {
                    if (category == DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM) {
                        return buildCategoryItem(category, null);
                    }

                    LocalDateTime genAppDate =
                        findLatestGenAppDateForCategory(pcsCaseEntity, category);

                    return genAppDate == null
                        ? null
                        : buildCategoryItem(category, genAppDate);
                })
                .filter(Objects::nonNull)
                .toList();

        caseData.getLegalRepDocumentUploadDetails().setValidCategories(
            DynamicStringList.builder()
                .listItems(validCategoryItems)
                .build()
        );
        return caseData;
    }

    private DynamicStringListElement buildCategoryItem(
        DocumentUploadCategory category,
        LocalDateTime genAppDate
    ) {
        return DynamicStringListElement.builder()
            .code(category.name())
            .label(category.getLabel(genAppDate))
            .build();
    }

    private LocalDateTime findLatestGenAppDateForCategory(PcsCaseEntity pcsCaseEntity,
                                                          DocumentUploadCategory category) {
        if (pcsCaseEntity == null || pcsCaseEntity.getGenApps() == null) {
            return null;
        }

        GenAppType mapped = mapCategoryToGenAppType(category);
        if (mapped == null) {
            return null;
        }

        return pcsCaseEntity.getGenApps().stream()
            .filter(genApp -> genApp.getType() == mapped)
            .map(GenAppEntity::getApplicationSubmittedDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private GenAppType mapCategoryToGenAppType(DocumentUploadCategory category) {
        return switch (category) {
            case ADJOURN_HEARING_APPLICATION -> GenAppType.ADJOURN;
            case SUSPEND_EVICTION_APPLICATION -> GenAppType.SUSPEND;
            case SET_ASIDE_ORDER_APPLICATION -> GenAppType.SET_ASIDE;
            case GENERAL_APPLICATION -> GenAppType.SOMETHING_ELSE;
            default -> null;
        };
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PCSCase pcsCase = eventPayload.caseData();
        LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = pcsCase.getLegalRepDocumentUploadDetails();
        if (legalRepDocumentUploadDetails != null) {
            List<LegalRepDocument> legalRepDocuments = legalRepDocumentUploadDetails.getLegalRepDocuments().stream()
                .map(ListValue::getValue).toList();

            List<DocumentEntity> documentEntities = legalRepDocuments.stream()
                .map(legalRepDoc -> DocumentEntity.builder()
                    .pcsCase(pcsCaseEntity)
                    .url(legalRepDoc.getDocument().getUrl())
                    .fileName(legalRepDoc.getDocument().getFilename())
                    .binaryUrl(legalRepDoc.getDocument().getBinaryUrl())
                    .categoryId(legalRepDoc.getDocument().getCategoryId())
                    .description(legalRepDoc.getDescription())
                    .type(mapEvidenceTypeToDocumentType(legalRepDoc.getDocumentType()))
                    .build())
                .toList();

            pcsCaseEntity.addDocuments(documentEntities);
        }
        return SubmitResponse.<State>builder()
            .confirmationBody(getDocumentUploadedConfirmationMarkdown())
            .build();
    }

    private DocumentType mapEvidenceTypeToDocumentType(EvidenceDocumentType evidenceDocumentType) {
        return switch (evidenceDocumentType) {
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case POLICE_REPORT -> DocumentType.POLICE_REPORT;
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case OTHER -> DocumentType.OTHER;
        };
    }

    private static String getDocumentUploadedConfirmationMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                <span class="govuk-panel__title govuk-!-font-size-36">Document uploaded</span>
            </div>
            <p class="govuk-body">We have received the documents you uploaded.</p>
             <h3>What happens next</h3>
            <p class="govuk-body">You do not need to do anything else. We will review the documents.</p>
            """;
    }
}

