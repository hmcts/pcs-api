package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A party filed this wording as the order it proposes. Filings are only ever added: a correction is another filing.
 * The case is the party's.
 */
@Entity
@Immutable
@Table(name = "submitted_draft_order")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmittedDraftOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    /** Rich text (HTML), cleaned when stored. */
    @Column(nullable = false)
    private String wording;

    /** The IDAM id of the user who filed it. */
    @Column(nullable = false)
    private String submittedBy;

    /** Set by the database when the filing is saved. */
    @Column(insertable = false, updatable = false)
    private Instant submittedAt;
}
