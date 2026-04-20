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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "flag_details")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagDetailsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private PcsCaseEntity pcsCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private PartyEntity party;

    @OneToMany(mappedBy = "flagDetails", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlagPathEntity> paths = new ArrayList<>();

    private String flagCode;

    private String name;

    @Column(name = "name_cy")
    private String nameWelsh;

    private String subTypeValue;

    @Column(name = "sub_type_value_cy")
    private String subTypeValueWelsh;

    private String subTypeKey;

    private String otherDescription;

    @Column(name = "other_description_cy")
    private String otherDescriptionWelsh;

    private Boolean hearingRelevant;

    private String flagComment;

    @Column(name = "flag_comment_cy")
    private String flagCommentWelsh;

    private String flagUpdateComment;

    private LocalDateTime dateTimeCreated;

    private LocalDateTime dateTimeModified;

    @Column(name = "status")
    private String defaultStatus;

    private Boolean availableExternally;
}

