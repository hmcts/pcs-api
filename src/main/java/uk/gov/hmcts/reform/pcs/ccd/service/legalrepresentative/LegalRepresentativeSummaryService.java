package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepresentativeContactDetails;

@Component
@RequiredArgsConstructor
public class LegalRepresentativeSummaryService {

    private static final String  RESPOND_TO_CLAIM_MARKDOWN = """
        <h2 class="govuk-heading-m">What happens next</h2>
        <p>
        <a href="%s/case/${[CASE_REFERENCE]}/respond-to-claim/start-now"
        role="button"
        class="govuk-link govuk-link--no-visited-state">
        Respond to the claim</a>.
        </p>
        """;
    private static final String UPDATE_DETAILS_MARKDOWN = """
        <h2 class="govuk-heading-m">What happens next</h2>
        <p>You must
        <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
        role="button"
        class="govuk-link govuk-link--no-visited-state">
        update the legal representative details for the case</a>
        before</p>
        <p>responding so you can receive updates and notifications
        about the case.
        </p>
        """;

    private final DefendantPartyExtractor defendantPartyExtractor;
    private final FeatureToggleService featureToggleService;
    private final ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void handleLegalRepresentativeSummary(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, State state,
                                                 Supplier<String> organisationId) {
        if (isFeatureDisabled()) {
            pcsCase.setSummaryLegalRepresentativeMarkdown(StringUtils.EMPTY);
            return;
        }

        Optional<ClaimPartyOrganisationEntity> partyLink =
            isActivelyLinkedToAnyDefendant(pcsCaseEntity, state, organisationId);

        if (displaySummaryLegalRepresentativeMarkdown(partyLink.isPresent(), state)) {
            setLegalRepresentativeFields(pcsCase, partyLink.get(), pcsCaseEntity.getCaseReference());
        } else {
            pcsCase.setSummaryLegalRepresentativeMarkdown(StringUtils.EMPTY);
        }
    }

    private void setLegalRepresentativeFields(PCSCase pcsCase,
                                                             ClaimPartyOrganisationEntity
                                                                 partyLink,
                                              long caseReference) {

        // Direct lookup: walking the organisation's contact details loads every case it has ever been
        // party to (one query per row), which is O(cases per organisation).
        YesOrNo hasAmendedContactDetails = claimPartyContactDetailsRepository
            .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                partyLink.getOrganisation().getOrganisationId(), caseReference)
            .map(ClaimPartyContactDetailsEntity::getContactDetailsCorrectConfirmation)
            .orElse(YesOrNo.NO);

        if (YesOrNo.YES.equals(hasAmendedContactDetails)) {
            pcsCase.setLegalRepUpdatedDetails(YesOrNo.YES);
            pcsCase.setSummaryLegalRepresentativeMarkdown(RESPOND_TO_CLAIM_MARKDOWN.formatted(frontendUrl));
        } else {
            pcsCase.setSummaryLegalRepresentativeMarkdown(UPDATE_DETAILS_MARKDOWN
                                                              .formatted(legalRepresentativeContactDetails));
        }
    }

    /**
     * Only resolves the viewer's organisation when the summary could show: an issued case with an active
     * legal-rep organisation on a defendant. Anyone else viewing the case costs no rd-professional lookup.
     */
    private Optional<ClaimPartyOrganisationEntity> isActivelyLinkedToAnyDefendant(PcsCaseEntity pcsCaseEntity,
                                                                                  State state,
                                                                                  Supplier<String> organisationId) {
        List<PartyEntity> defendants = defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity);
        List<ClaimPartyOrganisationEntity> activeLinks = defendants.stream()
            .flatMap(partyEntity -> partyEntity.getClaimPartyOrganisationList().stream())
            .filter(claimPartyLegalRepresentative -> YesOrNo.YES.equals(claimPartyLegalRepresentative.getActive()))
            .toList();

        if (activeLinks.isEmpty() || state != State.CASE_ISSUED) {
            return Optional.empty();
        }

        String orgId = organisationId.get();
        if (orgId == null) {
            return Optional.empty();
        }

        return activeLinks.stream()
            .filter(claimPartyLegalRepresentative ->
                        orgId.equals(claimPartyLegalRepresentative.getOrganisation().getOrganisationId()))
            .findFirst();
    }

    private boolean displaySummaryLegalRepresentativeMarkdown(boolean isPartyLink, State state) {
        return isPartyLink && state == State.CASE_ISSUED;
    }

    private boolean isFeatureDisabled() {
        return !featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)
            || !featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR);
    }


}
