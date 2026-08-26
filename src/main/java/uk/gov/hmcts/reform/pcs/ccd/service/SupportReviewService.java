package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;

import java.util.ArrayList;
import java.util.List;

@Service
public class SupportReviewService {

    public static final String REQUESTED_STATUS = "Requested";

    /**
     * Requested support is read from the party support collection, which is the only collection the
     * view layer populates with external party flags. The role-specific party collections carry no
     * external flags, so reading them here would always yield nothing to review.
     */
    public List<ListValue<PartySupport>> buildRequestedSupport(PCSCase pcsCase) {
        List<ListValue<PartySupport>> requestedSupport = new ArrayList<>();

        if (pcsCase.getPartySupport() == null) {
            return requestedSupport;
        }

        for (ListValue<PartySupport> partySupportValue : pcsCase.getPartySupport()) {
            PartySupport partySupport = partySupportValue.getValue();
            if (partySupport == null) {
                continue;
            }

            Flags supportFlags = partySupport.getSupportFlags();
            if (supportFlags == null || FlagVisibility.EXTERNAL != supportFlags.getVisibility()) {
                continue;
            }

            List<ListValue<FlagDetail>> requestedDetails = requestedDetails(supportFlags);
            if (requestedDetails.isEmpty()) {
                continue;
            }

            requestedSupport.add(ListValue.<PartySupport>builder()
                .id(partySupportValue.getId())
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
        if (flags.getDetails() == null) {
            return List.of();
        }

        return flags.getDetails().stream()
            .filter(detail -> detail.getValue() != null
                && REQUESTED_STATUS.equalsIgnoreCase(detail.getValue().getStatus()))
            .toList();
    }
}
