package uk.gov.hmcts.reform.pcs.sdp;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.sdp.model.ClaimSnapshot;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotDimensions;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotMilestones;
import uk.gov.hmcts.reform.pcs.sdp.model.SnapshotParty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotHasherTest {

    private final SnapshotHasher snapshotHasher = new SnapshotHasher(new JacksonConfiguration().getMapper());

    @Test
    void shouldReturnSameHashForSameClaim() {
        String hash = snapshotHasher.hash(claim(List.of(party("1", "a@example.com"), party("2", "b@example.com"))));

        assertThat(hash).hasSize(64);
        assertThat(snapshotHasher.hash(claim(List.of(party("1", "a@example.com"), party("2", "b@example.com")))))
            .isEqualTo(hash);
    }

    @Test
    void shouldIgnorePartyOrder() {
        String hash = snapshotHasher.hash(claim(List.of(party("1", "a@example.com"), party("2", "b@example.com"))));

        assertThat(snapshotHasher.hash(claim(List.of(party("2", "b@example.com"), party("1", "a@example.com")))))
            .isEqualTo(hash);
    }

    @Test
    void shouldChangeHashWhenClaimDataChanges() {
        String hash = snapshotHasher.hash(claim(List.of(party("1", "a@example.com"))));

        assertThat(snapshotHasher.hash(claim(List.of(party("1", "changed@example.com")))))
            .isNotEqualTo(hash);
    }

    @Test
    void shouldIgnoreSnapshotVersionAndRecordedDate() {
        ClaimSnapshot claim = claim(List.of(party("1", "a@example.com")));
        String hash = snapshotHasher.hash(claim);

        claim.setSnapshotVersion(5);
        claim.setRecordedDate("2026-03-01 10:00:00");

        assertThat(snapshotHasher.hash(claim)).isEqualTo(hash);
    }

    private static ClaimSnapshot claim(List<SnapshotParty> parties) {
        return ClaimSnapshot.builder()
            .claimReference("1234567890123456")
            .snapshotVersion(1)
            .recordedDate("2026-02-01 14:30")
            .dimensions(SnapshotDimensions.builder().claimType("AAA3-SLD").language("eng").build())
            .milestones(SnapshotMilestones.builder().claimSubmittedToSystemDate("2026-02-01").build())
            .parties(parties)
            .build();
    }

    private static SnapshotParty party(String partyId, String email) {
        return SnapshotParty.builder()
            .partyId(partyId)
            .role("DEFENDANT")
            .name("Jane Doe")
            .email(email)
            .build();
    }
}
