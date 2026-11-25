package uk.gov.hmcts.reform.pcs.ccd.entity.claim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;

import java.time.LocalDate;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/**
 * One of the ways a notice was served, with the date and details given for it.
 */
@Entity
@Table(name = "notice_service_method")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeServiceMethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "notice_of_possession_id", nullable = false)
    @JsonBackReference
    private NoticeOfPossessionEntity noticeOfPossession;

    @Enumerated(EnumType.STRING)
    private NoticeServiceMethod method;

    private LocalDate servedDate;

    private String details;

    private int displayOrder;

}
