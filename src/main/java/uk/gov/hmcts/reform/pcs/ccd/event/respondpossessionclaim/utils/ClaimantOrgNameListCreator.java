package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;

@Component
@Slf4j
public class ClaimantOrgNameListCreator {

    public List<ListValue<String>> createClaimantOrgNameList(PCSCase pcsCase) {
        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();

        if (allClaimants == null || allClaimants.isEmpty()) {
            log.warn("No claimant parties found");
            return List.of();
        }

        return allClaimants.stream()
            .map(claimant -> ListValue.<String>builder()
                .id(claimant.getId())
                .value(claimant.getValue().getOrgName())
                .build())
            .toList();
    }
}
