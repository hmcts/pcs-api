package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ref_data_flags")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDataFlagsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String flagName;

    @Column(name = "name_cy")
    private String flagNameWelsh;

    private String flagCode;

    private Boolean hearingRelevant;

    private Boolean availableExternally;

    private String visibility;
}
