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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "case_flag")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseFlagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    @OneToMany(mappedBy = "caseFlagEntity", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlagPathEntity> paths = new ArrayList<>();

    private String flagCode;

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


