package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@Component
@RequiredArgsConstructor
public class EnforcementOrderView {

    private final DraftCaseDataService draftCaseDataService;
    private final AddressFormatter addressFormatter;

    public void setCaseFields(PCSCase pcsCase, long caseReference, State state) {
        setHasEnforcementDraftData(caseReference, state, pcsCase);
        pcsCase.setFormattedPropertyAddress(
            addressFormatter.formatMediumAddress(pcsCase.getPropertyAddress(), BR_DELIMITER));
        setFormattedDefendantNames(pcsCase);
    }

    private void setHasEnforcementDraftData(long caseReference, State state, PCSCase pcsCase) {
        if (State.CASE_ISSUED == state) {
            boolean hasUnsubmittedCaseData = draftCaseDataService
                .hasUnsubmittedCaseData(caseReference, enforceTheOrder);
            if (hasUnsubmittedCaseData) {
                Optional<PCSCase> unsubmittedCaseData = draftCaseDataService
                    .getUnsubmittedCaseData(caseReference, enforceTheOrder);
                unsubmittedCaseData.ifPresent(inflated ->
                                                  pcsCase.setEnforcementOrder(inflated.getEnforcementOrder()));
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.YES);
            } else {
                pcsCase.setHasUnsubmittedEnforcementData(YesOrNo.NO);
            }
        }
    }

    public static void setFormattedDefendantNames(PCSCase pcsCase) {
        List<ListValue<Party>> defendants = pcsCase.getAllDefendants();
        if (defendants != null && !defendants.isEmpty()) {
            List<String> knownNames = new ArrayList<>();
            int unknownDefendants = 0;

            for (ListValue<Party> defendant : defendants) {
                Party party = defendant.getValue();
                if (isNameUnknown(party)) {
                    unknownDefendants++;
                    continue;
                }
                knownNames.add("<div class=\"govuk-!-margin-bottom-2\">"
                                   + party.getFirstName() + " " + party.getLastName()
                                   + "</div>");
            }

            pcsCase.setFormattedDefendantNames(StringUtils.join(knownNames, StringUtils.EMPTY));
            pcsCase.setUnknownDefendantCount(unknownDefendants);
        }
    }

    private static boolean isNameUnknown(Party party) {
        return StringUtils.isBlank(party.getFirstName()) && StringUtils.isBlank(party.getLastName());
    }
}
