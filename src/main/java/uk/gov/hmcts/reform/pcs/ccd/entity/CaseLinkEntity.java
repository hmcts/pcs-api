package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "case_link")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    @Column(name = "linked_case_reference", nullable = false)
    private Long linkedCaseReference;

    @Column(name = "ccd_list_id")
    private String ccdListId;

    @OneToMany(mappedBy = "caseLink",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @Builder.Default
    private List<CaseLinkReasonEntity> reasons = new ArrayList<>();
}
