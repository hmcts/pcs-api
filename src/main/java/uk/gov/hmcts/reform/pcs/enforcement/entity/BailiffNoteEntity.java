package uk.gov.hmcts.reform.pcs.enforcement.entity;

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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * A note that bailiff staff added about an enforcement. Notes are never edited.
 */
@Entity
@Table(name = "enf_bailiff_note")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BailiffNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "enf_case_id", nullable = false, updatable = false)
    private EnforcementOrderEntity enforcementOrder;

    @Column(name = "note", nullable = false, updatable = false, length = 6800)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "author_first_name", updatable = false)
    private String authorFirstName;

    @Column(name = "author_last_name", updatable = false)
    private String authorLastName;
}
