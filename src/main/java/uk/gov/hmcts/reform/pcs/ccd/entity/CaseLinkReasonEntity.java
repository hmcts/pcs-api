package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "case_link_reason")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseLinkReasonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_link_id")
    private CaseLinkEntity caseLink;

    @Column(name = "reason_code", nullable = false)
    private String reasonCode;

    @Column(name = "reason_text")
    private String reasonText;
}
