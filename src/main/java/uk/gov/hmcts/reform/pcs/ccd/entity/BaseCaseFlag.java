package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class BaseCaseFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pcs_case_id")
    private PcsCaseEntity pcsCase;

    @OneToMany(mappedBy = "caseFlagEntity", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlagPathEntity> caseFlagPaths = new ArrayList<>();

    @OneToMany(mappedBy = "casePartyFlagEntity", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlagPathEntity> casePartyFlagPaths = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flag_ref_data_id")
    private FlagRefDataEntity flagRefData;

    private String subTypeValue;

    @Column(name = "sub_type_value_cy")
    private String subTypeValueWelsh;

    private String subTypeKey;

    private String otherDescription;

    @Column(name = "other_description_cy")
    private String otherDescriptionWelsh;

    private String flagComment;

    @Column(name = "flag_comment_cy")
    private String flagCommentWelsh;

    private String flagUpdateComment;

    @Column(name = "flag_update_comment_cy")
    private String flagUpdateCommentWelsh;

    private LocalDateTime dateTimeCreated;

    private LocalDateTime dateTimeModified;

    @Column(name = "status")
    private String defaultStatus;
}
