package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Stream;

import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepDocumentUpload;

@Component
@AllArgsConstructor
public class LegalRepDocumentUpload implements CCDConfig<PCSCase, State, UserRole> {

    private final LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;
    private final PcsCaseService pcsCaseService;
    private final DocumentService documentService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final GenAppVisibilityService genAppVisibilityService;
    private final LegalRepForDefendantAccessValidator  legalRepForDefendantAccessValidator;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(legalRepDocumentUpload.name(), this::submit, this::start)
                .forAllStates()
                .name("Upload additional documents")
                .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
                .showSummary()
                .endButtonLabel("Submit");
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
                .flatMap(category -> {
                    if (category == DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM) {
                        return Stream.of(buildCategoryItem(category, null));
                    }

                    return findGenAppDatesForCategory(pcsCaseEntity, category)
                        .stream()
                        .map(date -> buildCategoryItem(category, date));
                })
                .toList();

        caseData.getLegalRepDocumentUploadDetails().setValidCategories(
            DynamicStringList.builder()
                .listItems(validCategoryItems)
                .build()
        );

        // By default, Main claim is always added
        caseData.getLegalRepDocumentUploadDetails().setShowExistingApplicationPage(validCategoryItems.size() >= 2
                                                                                       ? YesOrNo.YES : YesOrNo.NO);

        boolean isWalesClaim = pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES;
        caseData.getLegalRepDocumentUploadDetails().setIsWales(isWalesClaim ? YesOrNo.YES : YesOrNo.NO);

        return caseData;
    }

    DynamicStringListElement buildCategoryItem(
        DocumentUploadCategory category,
        LocalDateTime genAppDate
    ) {
        return DynamicStringListElement.builder()
            .code(category.name())
            .label(category.getLabel(genAppDate))
            .build();
    }

    List<LocalDateTime> findGenAppDatesForCategory(
        PcsCaseEntity pcsCaseEntity,
        DocumentUploadCategory category
    ) {
        if (pcsCaseEntity.getGenApps() == null) {
            return List.of();
        }

        GenAppType mapped = mapCategoryToGenAppType(category);
        if (mapped == null) {
            return List.of();
        }

        return pcsCaseEntity.getGenApps().stream()
            .filter(genApp -> genApp.getType() == mapped)
            .filter(genApp -> genApp.getWithoutNotice() != null
                && genApp.getWithoutNotice() == VerticalYesNo.YES)
            .map(GenAppEntity::getApplicationSubmittedDate)
            .filter(Objects::nonNull)
            .sorted(Comparator.reverseOrder()) // optional
            .toList();
    }

    GenAppType mapCategoryToGenAppType(DocumentUploadCategory category) {
        return switch (category) {
            case ADJOURN_HEARING_APPLICATION -> GenAppType.ADJOURN;
            case SET_ASIDE_ORDER_APPLICATION -> GenAppType.SET_ASIDE;
            case GENERAL_APPLICATION -> GenAppType.SOMETHING_ELSE;
            default -> null;
        };
    }

    private List<GenAppEntity> visibleGenAppsForUser(PcsCaseEntity pcsCaseEntity, UUID currentUserId) {
        return genAppVisibilityService.getVisibleGenAppsToUser(pcsCaseEntity.getGenApps(), currentUserId);
    }

    private GenAppEntity resolveSelectedGenApp(PCSCase caseData, PcsCaseEntity pcsCaseEntity, UUID currentUserId) {
        LegalRepDocumentUploadDetails details = caseData.getLegalRepDocumentUploadDetails();

        if (details == null || details.getSelectedLegalRepRelatedApplicationId() == null) {
            return null;
        }
        UUID selectedId;
        try {
            selectedId = UUID.fromString(details.getSelectedLegalRepRelatedApplicationId());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return visibleGenAppsForUser(pcsCaseEntity, currentUserId).stream()
            .filter(genApp -> selectedId.equals(genApp.getId()))
            .findFirst()
            .orElse(null);
    }

    private List<PartyEntity> loadAndValidateDefendants(PcsCaseEntity pcsCaseEntity) {

        return legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity,
                                                                            securityContextService.getCurrentUserId());
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PCSCase pcsCase = eventPayload.caseData();
        UUID currentUserId = securityContextService.getCurrentUserId();
        GenAppEntity selectedGenApp = resolveSelectedGenApp(pcsCase, pcsCaseEntity, currentUserId);
        PartyEntity party;

        if (selectedGenApp == null) {
            List<PartyEntity> partyEntities = loadAndValidateDefendants(pcsCaseEntity);
            if (partyEntities.size() == 1) {
                party = partyEntities.getFirst();
            } else {
                return errorResponse("Uploading documents for multiple parties is not supported");
            }
        } else {
            party = selectedGenApp.getParty();
        }

        List<LegalRepDocument> legalRepDocuments = documentService.createLegalRepDocuments(pcsCase);

        boolean isDocumentNull = legalRepDocuments.stream()
            .anyMatch(doc -> doc == null || doc.getDocument() == null);

        if (isDocumentNull) {
            return errorResponse("Your files were not submitted. Try again.");
        }

        documentService.createDocumentEntitiesFromLegalRepDocuments(legalRepDocuments,pcsCaseEntity,
                                                                    party,selectedGenApp);

        return SubmitResponse.<State>builder()
            .confirmationBody(getDocumentUploadedConfirmationMarkdown())
            .build();
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

    @SuppressWarnings("SameParameterValue")
    private SubmitResponse<State> errorResponse(String message) {
        return SubmitResponse.<State>builder()
            .errors(List.of(message))
            .build();
    }

}

