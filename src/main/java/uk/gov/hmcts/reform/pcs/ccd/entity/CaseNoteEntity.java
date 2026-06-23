package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@Entity
@Table(name = "case_note")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private String createdBy;

    @Column(updatable = false, nullable = false)
    private Instant createdOn;

    private String note;

    public static CaseNote fromEntity(uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity entity) {
        LocalDateTime ukDateTime = LocalDateTime.ofInstant(
                entity.getCreatedOn(),
                UK_ZONE_ID
        );

        return CaseNote.builder()
                .createdBy(entity.getCreatedBy())
                .createdOn(ukDateTime)  // Displays in BST/GMT automatically
                .note(entity.getNote())
                .build();
    }
}
