package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;


@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimantInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String forename;

    private String surname;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "parent_case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

}

