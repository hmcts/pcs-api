package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "pcs_case")
@Setter
@Getter
public class PcsCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    @OneToOne(cascade = ALL)
    private AddressEntity address;

    @OneToMany(cascade = ALL, mappedBy = "pcsCase")
    private Set<CasePossessionGround> possessionGrounds = new HashSet<>();

    private String generalNotes;

    public void addPossessionGround(CasePossessionGround possessionGround) {
        possessionGrounds.add(possessionGround);
        possessionGround.setPcsCase(this);
    }

}
