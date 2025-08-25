package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gen_app")
public class GenAppEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private Instant created;

    @Enumerated(EnumType.STRING)
    private GenAppState state;

    private String summary;

    @Column(insertable = false, updatable = false)
    private String genAppReference;

    @OneToMany(mappedBy = "genApp", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    @OrderBy("created desc")
    private List<GenAppEventLogEntity> eventLogs = new ArrayList<>();

    public void addClaimEventLog(GenAppEventLogEntity genAppEventLogEntity) {
        eventLogs.add(genAppEventLogEntity);
        genAppEventLogEntity.setGenApp(this);
    }

}
