package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.RespondToClaimCallbackError;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class SubmitResponseFactory {

    private static final String CORRESPONDENCE_ADDRESS_HINT = "correspondence address";

    private final AddressValidator addressValidator;

    public Optional<SubmitResponse<State>> validate(PossessionClaimResponse possessionClaimResponse,
                                                    long caseReference) {

        if (possessionClaimResponse == null) {
            log.error("Submit failed for case {}: possession claim response is null", caseReference);
            return Optional.of(error("Invalid submission: missing response data"));
        }

        if (possessionClaimResponse.getDefendantResponses() == null) {
            log.error("Submit failed for case {}: defendant responses is null", caseReference);
            return Optional.of(error("Invalid submission: missing defendant response data"));
        }

        List<String> addressErrors = validateEnteredCorrespondenceAddress(possessionClaimResponse);
        if (!addressErrors.isEmpty()) {
            log.error("Submit failed for case {}: {}", caseReference, addressErrors);
            return Optional.of(SubmitResponse.<State>builder().errors(addressErrors).build());
        }

        return Optional.empty();
    }

    public Optional<SubmitResponse<State>> validateDraftVersionNotChanged(EventPayload<PCSCase, State> eventPayload,
                                                                       PossessionClaimResponse storedDraft) {
        Long reviewedVersion = Optional.ofNullable(eventPayload.caseData())
            .map(PCSCase::getPossessionClaimResponse)
            .map(PossessionClaimResponse::getDraftVersion)
            .orElse(null);
        Long currentVersion = storedDraft.getDraftVersion();
        if (reviewedVersion == null || !reviewedVersion.equals(currentVersion)) {
            log.warn("Submit rejected for case {}: reviewed draft version {} but stored draft is at {}",
                     eventPayload.caseReference(), reviewedVersion, currentVersion);
            return Optional.of(error(RespondToClaimCallbackError.DRAFT_CHANGED));
        }
        return Optional.empty();
    }

    // An address the defendant typed in (answered No to the one shown) gets the same AddressValidator check as
    // the claimant and caseworker address pages; the draft itself is stored unvalidated.
    private List<String> validateEnteredCorrespondenceAddress(PossessionClaimResponse possessionClaimResponse) {
        DefendantResponses responses = possessionClaimResponse.getDefendantResponses();
        boolean enteredManually = responses.getCorrespondenceAddressConfirmation() == VerticalYesNo.NO
            || responses.getPropertyAddressConfirmation() == VerticalYesNo.NO;
        AddressUK address = Optional.ofNullable(possessionClaimResponse.getDefendantContactDetails())
            .map(DefendantContactDetails::getParty)
            .map(party -> party.getAddress())
            .orElse(null);
        if (!enteredManually || address == null) {
            return List.of();
        }
        return addressValidator.validateAddressFields(address, CORRESPONDENCE_ADDRESS_HINT);
    }

    public SubmitResponse<State> success() {
        return SubmitResponse.defaultResponse();
    }

    public SubmitResponse<State> error(String message) {
        return SubmitResponse.<State>builder()
            .errors(List.of(message))
            .build();
    }
}
