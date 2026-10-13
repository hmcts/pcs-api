package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.entity.JudicialNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.UserNameEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static jakarta.persistence.FetchType.LAZY;

/** What the judge at a hearing recorded: the case's facts as they stood then, and the hearing's note. */
@Entity
@Table(name = "hearing_facts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingFactsEntity {

    @Id
    private Integer hearingId;

    @MapsId
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "hearing_id")
    private HearingEntity hearing;

    @Version
    private long version;

    /** The judge who last recorded them. */
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserNameEntity user;

    @Column(nullable = false)
    private Instant updatedOn;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "judicial_note_id")
    private JudicialNoteEntity judicialNote;

    private LocalDate tenancyStartDate;
    private String tenancyType;
    private LocalDate noticeDate;
    private BigDecimal arrearsAtNotice;
    private BigDecimal arrearsAtIssue;
    private BigDecimal currentRent;
    private String rentFrequency;
    private String groundsPleaded;
    private LocalDate lastPaymentDate;
    private BigDecimal lastPayment;
    private BigDecimal arrearsToday;
}
