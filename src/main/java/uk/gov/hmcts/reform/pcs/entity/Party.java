package uk.gov.hmcts.reform.pcs.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * JPA Entity representing a party involved in a case.
 */
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String forename;
    private String surname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference", referencedColumnName = "reference")
    private PcsCase pcsCase;
}
