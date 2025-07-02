package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.GAType;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "general_application")
public class GACaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long caseReference;

    @Enumerated(EnumType.STRING)
    private GAType gaType;

    private String adjustment;

    private String additionalInformation;

    @Enumerated(EnumType.STRING)
    private State status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_case_id")
    private PcsCaseEntity pcsCase;
}
