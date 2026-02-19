package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.util.UUID;

@Entity
@Table(name = "case_link_reason")
@Getter
@Setter
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
