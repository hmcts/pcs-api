package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "flags")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    @OneToMany(mappedBy = "flags",
        cascade = ALL,
        orphanRemoval = true)
    @Builder.Default
    private List<FlagDetailsEntity> flagDetails = new ArrayList<>();

    @Column(name = "party_name")
    private String partyName;

    @Column(name = "role_on_case")
    private String roleOnCase;

    @Column(name = "visibility")
    private String visibility;

    @Column(name = "group_id")
    private String groupId;
}
