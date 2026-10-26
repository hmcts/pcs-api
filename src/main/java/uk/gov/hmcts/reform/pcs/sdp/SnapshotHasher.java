package uk.gov.hmcts.reform.pcs.sdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotGround;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotPaymentReference;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotResponse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@AllArgsConstructor
public class SnapshotHasher {

    private final ObjectMapper objectMapper;

    public String hash(ClaimSnapshot claim) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("claim_reference", claim.getClaimReference());
        content.put("dimensions", claim.getDimensions());
        content.put("milestones", claim.getMilestones());
        content.put("parties", sorted(claim.getParties(), SnapshotParty::getPartyId));
        content.put("payment_references",
            sorted(claim.getPaymentReferences(), SnapshotPaymentReference::getPaymentReference));
        content.put("grounds", sorted(claim.getGrounds(), SnapshotGround::getGroundsCode));
        content.put("responses", sorted(claim.getResponses(), SnapshotResponse::getResponseId));

        try {
            byte[] json = objectMapper.writeValueAsBytes(content);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash SDP claim snapshot", e);
        }
    }

    private static <T> List<T> sorted(List<T> values, Function<T, String> key) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .sorted(Comparator.comparing(key, Comparator.nullsFirst(Comparator.naturalOrder())))
            .toList();
    }
}
