package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.exception.MultiplePartiesException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepDocumentUpload;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.unwrapListItems;

@Component
@AllArgsConstructor
public class LegalRepDocumentUpload implements CCDConfig<PCSCase, State, UserRole> {

    private final LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;
    private final PcsCaseService pcsCaseService;
    private final DocumentService documentService;
    private final GenAppVisibilityService genAppVisibilityService;
    private final OrganisationService organisationService;
    private final LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    private final UserRoleService userRoleService;
    private final PartyService partyService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(legalRepDocumentUpload.name(), this::submit, this::start)
                .forAllStates()
                .name("Upload additional documents")
                .grant(Permission.CRUD, UserRole.CLAIMANT_SOLICITOR)
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
        String organisationId = organisationService.getOrganisationIdForCurrentUser();

        List<DynamicStringListElement> validCategoryItems =
            Arrays.stream(DocumentUploadCategory.values())
                .flatMap(category -> {
                    if (category == DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM) {
                        return Stream.of(buildCategoryItem(category, category.name(), null));
                    }

                    return findGenAppsForCategory(pcsCaseEntity, organisationId, category)
                        .stream()
                        .map(genApp -> buildCategoryItem(
                            category, genApp.getId().toString(), genApp.getApplicationSubmittedDate()));
                })
                .toList();

        LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = caseData.getLegalRepDocumentUploadDetails();
        legalRepDocumentUploadDetails.setValidCategories(
            DynamicStringList.builder()
                .listItems(validCategoryItems)
                .build()
        );

        // By default, Main claim is always added
        legalRepDocumentUploadDetails
            .setShowExistingApplicationPage(VerticalYesNo.from(validCategoryItems.size() >= 2));

        Collection<String> userRoles = userRoleService.getCurrentUserCaseRoles(caseReference).roles();
        boolean isClaimantSolicitor = isClaimantSolicitor(userRoles);
        legalRepDocumentUploadDetails.setPartyType(isClaimantSolicitor ? PartyType.CLAIMANT : PartyType.DEFENDANT);

        boolean isWalesClaim = pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES;
        legalRepDocumentUploadDetails.setIsWales(VerticalYesNo.from(isWalesClaim));

        return caseData;
    }

    private DynamicStringListElement buildCategoryItem(
        DocumentUploadCategory category,
        String code,
        LocalDateTime genAppDate
    ) {
        return DynamicStringListElement.builder()
            .code(code)
            .label(category.getLabel(genAppDate))
            .build();
    }

    List<GenAppEntity> findGenAppsForCategory(
        PcsCaseEntity pcsCaseEntity,
        String organisationId,
        DocumentUploadCategory category
    ) {
        GenAppType mapped = mapCategoryToGenAppType(category);
        if (mapped == null) {
            return List.of();
        }

        return visibleGenAppsForUser(pcsCaseEntity, organisationId).stream()
            .filter(genApp -> genApp.getType() == mapped)
            .filter(genApp -> genApp.getApplicationSubmittedDate() != null)
            .sorted(Comparator.comparing(GenAppEntity::getApplicationSubmittedDate).reversed())
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

    private List<GenAppEntity> visibleGenAppsForUser(PcsCaseEntity pcsCaseEntity, String organisationId) {
        return genAppVisibilityService.getVisibleGenAppsToUser(pcsCaseEntity.getGenApps(), organisationId);
    }

    private GenAppEntity resolveSelectedGenApp(PCSCase caseData, PcsCaseEntity pcsCaseEntity, String organisationId) {
        LegalRepDocumentUploadDetails details = caseData.getLegalRepDocumentUploadDetails();

        if (details == null || details.getValidCategories() == null) {
            return null;
        }
        String selectedCode = details.getValidCategories().getValueCode();
        if (selectedCode == null || selectedCode.equals(DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name())) {
            return null;
        }

        UUID selectedId = UUID.fromString(selectedCode);

        return visibleGenAppsForUser(pcsCaseEntity, organisationId).stream()
            .filter(genApp -> selectedId.equals(genApp.getId()))
            .findFirst()
            .orElse(null);
    }

    private List<PartyEntity> loadAndValidateDefendants(PcsCaseEntity pcsCaseEntity, String organisationId) {

        return legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity,
                                                                            organisationId);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PCSCase pcsCase = eventPayload.caseData();
        String organisationId = organisationService.getOrganisationIdForCurrentUser();
        GenAppEntity selectedGenApp = resolveSelectedGenApp(pcsCase, pcsCaseEntity, organisationId);

        PartyEntity uploadingParty;
        try {
            uploadingParty = getUploadingParty(caseReference, pcsCaseEntity, organisationId);
        } catch (MultiplePartiesException multiplePartiesException) {
            return errorResponse(multiplePartiesException.getMessage());
        }

        List<LegalRepDocument> legalRepDocuments
            = unwrapListItems(pcsCase.getLegalRepDocumentUploadDetails().getLegalRepDocuments());
        if (anyDocumentIsNull(legalRepDocuments)) {
            return errorResponse("Your files were not submitted. Try again.");
        }

        documentService.createDocumentEntitiesFromLegalRepDocuments(
            legalRepDocuments,
            pcsCaseEntity,
            uploadingParty,
            selectedGenApp
        );

        return SubmitResponse.<State>builder()
            .confirmationBody(getDocumentUploadedConfirmationMarkdown())
            .build();
    }

    private PartyEntity getUploadingParty(long caseReference,
                                          PcsCaseEntity pcsCaseEntity,
                                          String organisationId) {

        Collection<String> userRoles = userRoleService.getCurrentUserCaseRoles(caseReference).roles();
        boolean isClaimantSolicitor = isClaimantSolicitor(userRoles);
        boolean isDefendantSolicitor = isDefendantSolicitor(userRoles);
        if (isClaimantSolicitor && isDefendantSolicitor) {
            throw new MultiplePartiesException("Uploading documents for multiple parties is not supported");
        }

        if (isClaimantSolicitor) {
            return partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity);
        } else {
            List<PartyEntity> partyEntities = loadAndValidateDefendants(pcsCaseEntity, organisationId);
            if (partyEntities.size() == 1) {
                return partyEntities.getFirst();
            } else {
                throw new MultiplePartiesException("Uploading documents for multiple parties is not supported");
            }
        }
    }

    private static boolean isClaimantSolicitor(Collection<String> userRoles) {
        return userRoles.contains(UserRole.CLAIMANT_SOLICITOR.getRole());
    }

    private static boolean isDefendantSolicitor(Collection<String> userRoles) {
        return userRoles.contains(UserRole.DEFENDANT_SOLICITOR.getRole());
    }

    private static boolean anyDocumentIsNull(List<LegalRepDocument> legalRepDocuments) {
        return legalRepDocuments.stream()
            .anyMatch(doc -> doc == null || doc.getDocument() == null);
    }

    private static String getDocumentUploadedConfirmationMarkdown() {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
                <span class="govuk-panel__title govuk-!-font-size-36">Documents uploaded</span>
            </div>
            <p class="govuk-body">We have received the documents you uploaded.</p>
             <h3>What happens next</h3>
            <p class="govuk-body">You do not need to do anything else. We will review the documents.</p>
            """;
    }

    private SubmitResponse<State> errorResponse(String message) {
        return SubmitResponse.<State>builder()
            .errors(List.of(message))
            .build();
    }
}
