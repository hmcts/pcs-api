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
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
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
                .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
                .showSummary();
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

        // By default, Main claim is always added
        caseData.getLegalRepDocumentUploadDetails().setShowExistingApplicationPage(validCategoryItems.size() >= 2
                                                                                       ? YesOrNo.YES : YesOrNo.NO);
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

    LocalDateTime findLatestGenAppDateForCategory(PcsCaseEntity pcsCaseEntity,
                                                          DocumentUploadCategory category) {
        if (pcsCaseEntity.getGenApps() == null) {
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

    GenAppType mapCategoryToGenAppType(DocumentUploadCategory category) {
        return switch (category) {
            case ADJOURN_HEARING_APPLICATION -> GenAppType.ADJOURN;
            case SET_ASIDE_ORDER_APPLICATION -> GenAppType.SET_ASIDE;
            case GENERAL_APPLICATION -> GenAppType.SOMETHING_ELSE;
            default -> null;
        };
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }
}
