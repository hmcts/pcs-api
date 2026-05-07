package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "flag_ref_data")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagRefDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String flagName;

    @Column(name = "name_cy")
    private String flagNameWelsh;

    @OneToMany(mappedBy = "flagRefData", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseFlagEntity> caseFlags = new ArrayList<>();

    private String flagCode;

    private Boolean hearingRelevant;

    private Boolean availableExternally;

    private String visibility;
}
