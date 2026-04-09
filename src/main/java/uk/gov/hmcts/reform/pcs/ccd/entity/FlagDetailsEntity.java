package uk.gov.hmcts.reform.pcs.ccd.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

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
    @JoinColumn(name = "flags_id")
    private FlagsEntity flags;

    @Column(name = "flag_code")
    private String flagCode;

    @Column(name = "name")
    private String name;

    @Column(name = "name_cy")
    private String nameWelsh;

    @Column(name = "sub_type_value")
    private String subTypeValue;

    @Column(name = "sub_type_value_cy")
    private String subTypeValueWelsh;

    @Column(name = "sub_type_key")
    private String subTypeKey;

    @Column(name = "other_description")
    private String otherDescription;

    @Column(name = "hearing_relevant")
    private Boolean hearingRelevant;

    @Column(name = "flag_comment")
    private String flagComment;

    @Column(name = "flag_comment_cy")
    private String flagCommentWelsh;

    @Column(name = "flag_update_comment")
    private String flagUpdateComment;

    @Column(name = "date_time_created")
    private LocalDateTime dateTimeCreated;

    @Column(name = "date_time_modified")
    private LocalDateTime dateTimeModified;


    @Column(name = "path")
    private String path;

    @Column(name = "status")
    private String defaultStatus;

    @Column(name = "available_externally")
    private Boolean availableExternally;
}


