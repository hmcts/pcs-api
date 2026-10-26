package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class SnapshotHasher {

    private final ObjectMapper objectMapper;

    public String hash(ClaimSnapshot claim) {
        List<SnapshotParty> parties = claim.getParties().stream()
            .sorted(Comparator.comparing(SnapshotParty::getPartyId,
                                         Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("claim_reference", claim.getClaimReference());
        content.put("dimensions", claim.getDimensions());
        content.put("milestones", claim.getMilestones());
        content.put("parties", parties);

        try {
            byte[] json = objectMapper.writeValueAsBytes(content);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash SDP claim snapshot", e);
        }
    }
}
