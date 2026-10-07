package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.translation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UploadedDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TranslatedDocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ManageDocumentStates.MANAGE_DOCUMENT_STATES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.TranslationRoles.TRANSLATION_ROLES;


@Component
@Slf4j
@RequiredArgsConstructor
public class UploadTranslatedClaimForm implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final DocumentIdExtractor documentIdExtractor;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {

        configBuilder
            .decentralisedEvent("ext:" + EventId.uploadTranslatedClaimForm.name(), this::submit)
            .forStates(MANAGE_DOCUMENT_STATES)// TODO: Adjust this?
            .name("Upload Translated Claim Form")
            .description("Upload translated claim form")
            .showCondition(ShowConditions.NEVER_SHOW)
            .showSummary()
            .endButtonLabel("Submit")
            .grant(Permission.CRU, TRANSLATION_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);

    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();

        List<ListValue<UploadedDocument>> translatedDocuments = caseData.getTranslatedDocuments();

        translatedDocuments.stream()
            .map(ListValue::getValue)
            .map(translatedDocument -> createDocumentEntity(translatedDocument.getDocument()))
            .forEach(documentEntity -> {
                pcsCaseEntity.addDocument(documentEntity);

                TranslatedDocumentEntity translatedDocumentEntity = TranslatedDocumentEntity.builder()
                    .document(documentEntity)
                    .claim(mainClaim)
                    .build();
                mainClaim.getTranslatedDocuments().add(translatedDocumentEntity);
            });

        return SubmitResponse.<State>builder().build();
    }

    private DocumentEntity createDocumentEntity(Document document) {
        return DocumentEntity.builder()
            .url(document.getUrl())
            .binaryUrl(document.getBinaryUrl())
            .documentId(documentIdExtractor.extractDocumentId(document.getUrl()))
            .categoryId(CaseFileCategory.STATEMENTS_OF_CASE.getId())
            .fileName(document.getFilename())
            .type(DocumentType.CLAIM)
            .build();
    }

}
