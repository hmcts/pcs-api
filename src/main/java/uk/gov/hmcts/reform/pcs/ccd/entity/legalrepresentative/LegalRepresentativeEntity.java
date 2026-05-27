package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "legal_representative")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalRepresentativeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID idamId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "organisation_id")
    @JsonBackReference
    private LegalRepresentativeOrganisationEntity legalRepresentativeOrganisation;

}
