package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "case_link",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_case_link_unique",
        columnNames = {"pcs_case_id", "linked_case_reference"}
    ))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CaseLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")  // must match table column exactly
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
        CaseLinkReasonEntity r = new CaseLinkReasonEntity();
        r.setCaseLink(this);
        r.setReasonCode(code);
        r.setReasonText(text);
        reasons.add(r);
    }
}
