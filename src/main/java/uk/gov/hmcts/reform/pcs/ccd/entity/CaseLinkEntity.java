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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "case_link",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_case_link_unique",
        columnNames = {"pcs_case_id", "linked_case_reference"}
    ))
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

    @Column(name = "linked_case_id", nullable = false)
    private Long linkedCaseReference;

    @Column(name = "ccd_list_id")
    private String ccdListId;

    @OneToMany(mappedBy = "caseLink",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @Builder.Default
    private List<CaseLinkReasonEntity> reasons = new ArrayList<>();

    public void addReason(String code, String text) {
        CaseLinkReasonEntity caseLinkReasonEntity = new CaseLinkReasonEntity();
        caseLinkReasonEntity.setCaseLink(this);
        caseLinkReasonEntity.setReasonCode(code);
        caseLinkReasonEntity.setReasonText(text);
        reasons.add(caseLinkReasonEntity);

    }
}
