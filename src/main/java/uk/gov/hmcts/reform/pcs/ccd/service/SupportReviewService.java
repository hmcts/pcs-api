package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SupportReviewService {

    public static final String REQUESTED_STATUS = "Requested";

    public List<ListValue<PartySupport>> buildRequestedSupport(PCSCase pcsCase) {
        Map<String, RequestedSupport> requestedByPartyId = new LinkedHashMap<>();

        collectFromSupportProjection(pcsCase, requestedByPartyId);
        collectFromPartyCollections(pcsCase, requestedByPartyId);

        List<ListValue<PartySupport>> requestedSupport = new ArrayList<>();
        requestedByPartyId.forEach((partyId, requested) -> requestedSupport.add(
            ListValue.<PartySupport>builder()
                .id(partyId)
                .value(PartySupport.builder()
                    .supportFlags(requested.toFlags())
                    .build())
                .build()));

        return requestedSupport;
    }

    private void collectFromSupportProjection(PCSCase pcsCase, Map<String, RequestedSupport> requestedByPartyId) {
        if (pcsCase.getPartySupport() == null) {
            return;
        }

        for (ListValue<PartySupport> partySupportValue : pcsCase.getPartySupport()) {
            PartySupport partySupport = partySupportValue.getValue();
            if (partySupport == null) {
                continue;
            }

            collect(partySupportValue.getId(), partySupport.getSupportFlags(), requestedByPartyId);
        }
    }

    private void collectFromPartyCollections(PCSCase pcsCase, Map<String, RequestedSupport> requestedByPartyId) {
        if (pcsCase.getParties() == null) {
            return;
        }

        for (ListValue<Party> partyValue : pcsCase.getParties()) {
            Party party = partyValue.getValue();
            if (party == null) {
                continue;
            }

            collect(partyValue.getId(), party.getDefendantFlags(), requestedByPartyId);
            collect(partyValue.getId(), party.getPartyFlagsExternal(), requestedByPartyId);
        }
    }

    private void collect(String partyId, Flags flags, Map<String, RequestedSupport> requestedByPartyId) {
        if (partyId == null || flags == null) {
            return;
        }

        List<ListValue<FlagDetail>> requestedDetails = requestedDetails(flags);
        if (requestedDetails.isEmpty()) {
            return;
        }

        requestedByPartyId
            .computeIfAbsent(partyId, key -> new RequestedSupport(flags))
            .addAll(requestedDetails);
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

    private static final class RequestedSupport {

        private final Flags source;
        private final Set<FlagDetailKey> seen = new LinkedHashSet<>();
        private final List<ListValue<FlagDetail>> details = new ArrayList<>();

        private RequestedSupport(Flags source) {
            this.source = source;
        }

        private void addAll(List<ListValue<FlagDetail>> requestedDetails) {
            for (ListValue<FlagDetail> detail : requestedDetails) {
                if (seen.add(FlagDetailKey.of(detail))) {
                    details.add(detail);
                }
            }
        }

        private Flags toFlags() {
            return Flags.builder()
                .partyName(source.getPartyName())
                .roleOnCase(source.getRoleOnCase())
                .groupId(source.getGroupId())
                .visibility(FlagVisibility.INTERNAL)
                .details(details)
                .build();
        }
    }

    private record FlagDetailKey(String id, String flagCode, String subTypeValue, String otherDescription) {

        private static FlagDetailKey of(ListValue<FlagDetail> detail) {
            FlagDetail value = detail.getValue();
            if (detail.getId() != null) {
                return new FlagDetailKey(detail.getId(), null, null, null);
            }

            return new FlagDetailKey(null, value.getFlagCode(), value.getSubTypeValue(),
                                     value.getOtherDescription());
        }
    }
}
