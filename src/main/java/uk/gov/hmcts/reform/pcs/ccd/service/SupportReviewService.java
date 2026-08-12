package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupportReviewService {

    public static final String REQUESTED_STATUS = "Requested";

    public List<ListValue<PartySupport>> buildRequestedSupport(PCSCase pcsCase) {
        List<ListValue<PartySupport>> requestedSupport = new ArrayList<>();

        if (pcsCase.getAllDefendants() == null) {
            return requestedSupport;
        }

        for (ListValue<Party> partyListValue : pcsCase.getAllDefendants()) {
            Party party = partyListValue.getValue();
            if (party == null) {
                continue;
            }

            List<ListValue<FlagDetail>> requestedDetails = requestedDetails(party.getPartyFlagsExternal());
            if (requestedDetails.isEmpty()) {
                continue;
            }

            Flags supportFlags = party.getPartyFlagsExternal();

            requestedSupport.add(ListValue.<PartySupport>builder()
                .id(partyListValue.getId())
                .value(PartySupport.builder()
                    .supportFlags(Flags.builder()
                        .partyName(supportFlags.getPartyName())
                        .roleOnCase(supportFlags.getRoleOnCase())
                        .groupId(supportFlags.getGroupId())
                        .visibility(supportFlags.getVisibility())
                        .details(requestedDetails)
                        .build())
                    .build())
                .build());
        }

        return requestedSupport;
    }

    private List<ListValue<FlagDetail>> requestedDetails(Flags flags) {
        if (flags == null || flags.getDetails() == null) {
            return List.of();
        }

        return flags.getDetails().stream()
            .filter(detail -> detail.getValue() != null
                && REQUESTED_STATUS.equalsIgnoreCase(detail.getValue().getStatus()))
            .toList();
    }
}
