package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A letter Send Letter accepted, and where it has got to. Who it went to and what it carried are on the
 * {@code PACK_SENT} activity rows that point at it.
 */
@Entity
@Table(name = "letter")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterEntity {

    @Id
    @Column(name = "letter_id")
    private UUID letterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LetterStatus status;

    @Column(name = "sent_to_print_at")
    private LocalDateTime sentToPrintAt;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    // When Send Letter last answered for this letter.
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    // When PCS last asked, answered or not; orders the sync queue so a failing letter goes to the back.
    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;
}
