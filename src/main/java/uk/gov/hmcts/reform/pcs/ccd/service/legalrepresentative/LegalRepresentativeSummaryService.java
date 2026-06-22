package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantPartyExtractor;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;
import java.util.Optional;

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
        Respond to the claim</a>
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

    private final OrganisationService organisationService;
    private final DefendantPartyExtractor defendantPartyExtractor;

   // @Value("${frontend.url}")
    private String frontendUrl;

    public void handleLegalRepresentativeSummary(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        Optional<PartyLegalRepresentativeOrganisationEntity> partyLink =
            isActivelyLinkedToAnyDefendant(pcsCaseEntity);

        if (partyLink.isPresent()) {
            setLegalRepresentativeFields(pcsCase, partyLink.get());
        } else {
            pcsCase.setSummaryLegalRepresentativeMarkdown(StringUtils.EMPTY);
        }
    }

    private void setLegalRepresentativeFields(PCSCase pcsCase,
                                                             PartyLegalRepresentativeOrganisationEntity
                                                                 partyLink) {
        YesOrNo hasAmendedContactDetails = partyLink.getLegalRepresentativeOrganisation().getHasAmendedContactDetails();
        if (YesOrNo.YES.equals(hasAmendedContactDetails)) {
            pcsCase.setLegalRepUpdatedDetails(YesOrNo.YES);
            pcsCase.setSummaryLegalRepresentativeMarkdown(RESPOND_TO_CLAIM_MARKDOWN.formatted(frontendUrl));
        } else {
            pcsCase.setSummaryLegalRepresentativeMarkdown(UPDATE_DETAILS_MARKDOWN
                                                              .formatted(legalRepresentativeContactDetails));
        }
    }

    private Optional<PartyLegalRepresentativeOrganisationEntity> isActivelyLinkedToAnyDefendant(PcsCaseEntity
                                                                                                    pcsCaseEntity) {
        List<PartyEntity> defendants = defendantPartyExtractor.summaryScreenSafeExtractDefendants(pcsCaseEntity);
        return defendants.stream()
            .flatMap(partyEntity -> partyEntity.getPartyLegalRepresentativeOrganisationList().stream())
            .filter(claimPartyLegalRepresentative ->
                        claimPartyLegalRepresentative.getLegalRepresentativeOrganisation()
                            .getOrganisationId().equals(
                                organisationService.getOrganisationIdForCurrentUser())
                            && claimPartyLegalRepresentative.getActive().equals(YesOrNo.YES))
            .findFirst();
    }


}
