package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.CaseView;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseTitleService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.view.AlternativesToPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.AsbProhibitedConductView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseFlagsView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseLinkView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseListView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseNoteView;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimGroundsView;
import uk.gov.hmcts.reform.pcs.ccd.view.ClaimView;
import uk.gov.hmcts.reform.pcs.ccd.view.DefendantResponseView;
import uk.gov.hmcts.reform.pcs.ccd.view.DocumentsView;
import uk.gov.hmcts.reform.pcs.ccd.view.FeatureFlagView;
import uk.gov.hmcts.reform.pcs.ccd.view.GenAppsView;
import uk.gov.hmcts.reform.pcs.ccd.view.NoticeOfPossessionView;
import uk.gov.hmcts.reform.pcs.ccd.view.PartiesView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentArrearsView;
import uk.gov.hmcts.reform.pcs.ccd.view.RentDetailsView;
import uk.gov.hmcts.reform.pcs.ccd.view.StatementOfTruthView;
import uk.gov.hmcts.reform.pcs.ccd.view.TenancyLicenceView;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.CaseFieldsView;
import uk.gov.hmcts.reform.pcs.ccd.view.globalsearch.SearchCriteriaIndexer;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class PCSCaseView implements CaseView<PCSCase, State> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final DraftCaseDataService draftCaseDataService;
    private final CaseTitleService caseTitleService;
    private final ClaimView claimView;
    private final DocumentsView documentsView;
    private final TenancyLicenceView tenancyLicenceView;
    private final ClaimGroundsView claimGroundsView;
    private final RentDetailsView rentDetailsView;
    private final AlternativesToPossessionView alternativesToPossessionView;
    private final AsbProhibitedConductView asbProhibitedConductView;
    private final RentArrearsView rentArrearsView;
    private final NoticeOfPossessionView noticeOfPossessionView;
    private final StatementOfTruthView statementOfTruthView;
    private final CaseFieldsView caseFieldsView;
    private final SearchCriteriaIndexer searchCriteriaIndexer;
    private final CaseListView caseListView;
    private final CaseLinkView caseLinkView;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final CaseNoteView caseNoteView;
    private final CaseTabView caseTabView;
    private final PartiesView partiesView;
    private final GenAppsView genAppsView;
    private final CaseFlagsView flagsView;
    private final DefendantResponseView defendantResponseView;
    private final FeatureFlagView featureFlagView;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param request encapsulates the CCD case reference and state
     */
    @Override
    @Transactional(readOnly = true)
    public PCSCase getCase(CaseViewRequest<State> request) {
        long caseReference = request.caseRef();
        State state = request.state();
        SubmittedCase submittedCase = getSubmittedCase(caseReference);
        PCSCase pcsCase = submittedCase.pcsCase();
        boolean hasUnsubmittedCaseData = caseHasUnsubmittedData(caseReference, state);

        if (hasUnsubmittedCaseData) {
            draftCaseDataService
                .getUnsubmittedCaseData(caseReference, resumePossessionClaim)
                .ifPresentOrElse(
                    draft -> {
                        caseTabView.setDraftCaseTabFields(pcsCase, draft);
                        },
                    () -> caseTabView.setCaseTabFields(pcsCase)
                );
        } else {
            caseTabView.setCaseTabFields(pcsCase);
        }

        setMarkdownFields(pcsCase, hasUnsubmittedCaseData);
        enforcementOrderMediator.handleEnforcementRequirements(submittedCase.pcsCaseEntity(), pcsCase);

        caseFieldsView.setCaseFields(pcsCase);

        // Only the canonical PCS case type is indexed into the shared global_search index.
        if (!CaseType.isSuffixedCaseType()) {
            pcsCase.setSearchCriteria(searchCriteriaIndexer.buildSearchCriteria(pcsCase));
        }

        return pcsCase;
    }

    private boolean caseHasUnsubmittedData(long caseReference, State state) {
        if (State.AWAITING_SUBMISSION_TO_HMCTS == state) {
            return draftCaseDataService.hasUnsubmittedCaseData(caseReference, resumePossessionClaim);
        }

        return false;
    }

    private SubmittedCase getSubmittedCase(long caseReference) {
        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .legislativeCountry(pcsCaseEntity.getLegislativeCountry())
            .caseManagementLocationNumber(pcsCaseEntity.getBaseLocation())
            .regionId(pcsCaseEntity.getRegionId())
            .dateSubmitted(getClaimSubmittedDate(pcsCaseEntity))
            .dateIssued(getClaimIssuedDate(pcsCaseEntity))
            .claimIssueDate(getClaimIssueDateLocal(pcsCaseEntity))
            .build();

        setDerivedProperties(pcsCase, pcsCaseEntity);

        partiesView.setCaseFields(pcsCase, pcsCaseEntity);
        claimView.setCaseFields(pcsCase, pcsCaseEntity);
        documentsView.setCaseFields(pcsCase, pcsCaseEntity);
        tenancyLicenceView.setCaseFields(pcsCase, pcsCaseEntity);
        claimGroundsView.setCaseFields(pcsCase, pcsCaseEntity);
        rentDetailsView.setCaseFields(pcsCase, pcsCaseEntity);
        alternativesToPossessionView.setCaseFields(pcsCase, pcsCaseEntity);
        asbProhibitedConductView.setCaseFields(pcsCase, pcsCaseEntity);

        rentArrearsView.setCaseFields(pcsCase, pcsCaseEntity);
        noticeOfPossessionView.setCaseFields(pcsCase, pcsCaseEntity);
        statementOfTruthView.setCaseFields(pcsCase, pcsCaseEntity);
        genAppsView.setCaseFields(pcsCase, pcsCaseEntity);
        caseLinkView.setCaseFields(pcsCase, pcsCaseEntity);
        caseNoteView.setCaseFields(pcsCase, pcsCaseEntity);
        flagsView.setCaseFields(pcsCase, pcsCaseEntity);
        caseListView.setCaseFields(pcsCase);
        defendantResponseView.setCaseFields(pcsCase, pcsCaseEntity);
        featureFlagView.setCaseFields(pcsCase);
        removeDocumentsAlreadyPresentInOtherCaseFields(pcsCase);

        return new SubmittedCase(pcsCase, pcsCaseEntity);
    }

    private void removeDocumentsAlreadyPresentInOtherCaseFields(PCSCase pcsCase) {
        List<ListValue<Document>> allDocuments = pcsCase.getAllDocuments();

        if (allDocuments == null || allDocuments.isEmpty()) {
            return;
        }

        Set<String> documentIdsInOtherFields = findDocumentIdsOutsideAllDocuments(pcsCase);

        pcsCase.setAllDocuments(allDocuments.stream()
                                    .filter(document -> !documentIdsInOtherFields.contains(document.getId()))
                                    .toList());
    }

    private Set<String> findDocumentIdsOutsideAllDocuments(PCSCase pcsCase) {
        Set<String> documentIds = new HashSet<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Field field : PCSCase.class.getDeclaredFields()) {
            if ("allDocuments".equals(field.getName())) {
                continue;
            }

            field.trySetAccessible();
            try {
                collectDocumentIds(field.get(pcsCase), documentIds, visited);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to inspect PCSCase document fields", e);
            }
        }

        return documentIds;
    }

    private void collectDocumentIds(Object value, Set<String> documentIds, Set<Object> visited) {
        if (value == null || isSimpleValue(value) || value instanceof Document || !visited.add(value)) {
            return;
        }

        if (value instanceof DocumentWithId documentWithId) {
            addDocumentId(documentIds, documentWithId.getId());
            return;
        }

        if (value instanceof ListValue<?> listValue) {
            if (listValue.getValue() instanceof Document) {
                addDocumentId(documentIds, listValue.getId());
            } else {
                collectDocumentIds(listValue.getValue(), documentIds, visited);
            }
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> collectDocumentIds(item, documentIds, visited));
            return;
        }

        if (value instanceof Map<?, ?> map) {
            map.forEach((key, mapValue) -> {
                collectDocumentIds(key, documentIds, visited);
                collectDocumentIds(mapValue, documentIds, visited);
            });
            return;
        }

        if (!value.getClass().getPackageName().startsWith("uk.gov.hmcts.reform.pcs")) {
            return;
        }

        for (Field field : value.getClass().getDeclaredFields()) {
            field.trySetAccessible();
            try {
                collectDocumentIds(field.get(value), documentIds, visited);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to inspect PCSCase document fields", e);
            }
        }
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof UUID
            || value instanceof Temporal;
    }

    private void addDocumentId(Set<String> documentIds, String documentId) {
        if (documentId != null) {
            documentIds.add(documentId);
        }
    }

    private LocalDateTime getClaimSubmittedDate(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst()
            .map(ClaimEntity::getClaimSubmittedDate)
            .orElse(null);
    }

    private LocalDateTime getClaimIssuedDate(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst()
            .map(ClaimEntity::getClaimIssuedDate)
            .orElse(null);
    }

    private LocalDate getClaimIssueDateLocal(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst()
            .map(ClaimEntity::getClaimIssuedDate)
            .map(issued -> issued.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(UK_ZONE_ID)
                .toLocalDate())
            .orElse(null);
    }

    private void setDerivedProperties(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));

        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));
    }

    private void setMarkdownFields(PCSCase pcsCase, boolean hasUnsubmittedCaseData) {
        pcsCase.setCaseTitleMarkdown(caseTitleService.buildCaseTitle(pcsCase));

        if (hasUnsubmittedCaseData) {
            pcsCase.setNextStepsMarkdown("""
                                             <h2 class="govuk-heading-m">Resume claim</h2>
                                             You've already answered some questions about this claim.
                                             <br>
                                             <br>
                                             <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
                                                role="button"
                                                class="govuk-button govuk-link govuk-link--no-visited-state">
                                               Continue
                                             </a>
                                             <p class="govuk-body govuk-!-font-size-19">
                                             <span><a class="govuk-link--no-visited-state" href="/cases">Cancel</a></span>
                                             </p>
                                             """.formatted(resumePossessionClaim));
        } else {
            pcsCase.setNextStepsMarkdown("""
                                             <h2 class="govuk-heading-m">Provide more details about your claim</h2>
                                             Your answers will be saved from this point so you can return to your draft
                                             later.
                                             <br>
                                             <br>
                                             <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
                                                role="button"
                                                class="govuk-button govuk-link govuk-link--no-visited-state">
                                               Continue
                                             </a>
                                             <p class="govuk-body govuk-!-font-size-19">
                                             <span><a class="govuk-link--no-visited-state" href="/cases">Cancel</a></span>
                                             </p>
                                             """.formatted(resumePossessionClaim));
        }
    }

    private Optional<PartyEntity> findPartyForCurrentUser(PcsCaseEntity pcsCaseEntity) {
        UUID userId = securityContextService.getCurrentUserId();

        if (userId != null) {
            return pcsCaseEntity.getParties().stream()
                .filter(party -> userId.equals(party.getIdamId()))
                .findFirst();
        } else {
            return Optional.empty();
        }
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }

        return modelMapper.map(address, AddressUK.class);
    }

    private PcsCaseEntity loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCaseReference(caseRef)
            .orElseThrow(() -> new CaseNotFoundException(caseRef));
    }

    private List<ListValue<Party>> mapAndWrapParties(Set<PartyEntity> partyEntities) {
        return partyEntities.stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .collect(Collectors.collectingAndThen(Collectors.toList(), ListValueUtils::wrapListItems));
    }

    private record SubmittedCase(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
    }

}
