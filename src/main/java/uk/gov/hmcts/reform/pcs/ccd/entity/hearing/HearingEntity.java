package uk.gov.hmcts.reform.pcs.ccd.entity.hearing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.domain.HearingNoticeWording;
import uk.gov.hmcts.reform.pcs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hearing")
public class HearingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    @OneToMany(fetch = LAZY, mappedBy = "hearing")
    @Builder.Default
    @JsonManagedReference
    private Set<HearingPartyEntity> hearingParties = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private HearingType type;

    private String otherHearingType;

    @Enumerated(EnumType.STRING)
    private HearingNoticeWording noticeWording;

    private LocalDateTime date;

    private Integer durationHours;

    private Integer durationMinutes;

    private String notes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo noticeIssued;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VerticalYesNo isWithoutNotice;

    private String additionalInformation;

    public void addParty(PartyEntity party) {
        HearingPartyEntity hearingPartyEntity = HearingPartyEntity.builder()
            .hearing(this)
            .party(party)
            .build();
        hearingParties.add(hearingPartyEntity);
        party.getHearingParties().add(hearingPartyEntity);
    }

}
