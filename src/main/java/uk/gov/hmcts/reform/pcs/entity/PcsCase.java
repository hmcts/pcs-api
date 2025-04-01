package uk.gov.hmcts.reform.pcs.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

/**
 * JPA Entity representing a possessions case.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PcsCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    @OneToOne(mappedBy = "pcsCase", cascade = ALL)
    private Address address;

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<Party> parties = new HashSet<>();

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<Claim> claims = new HashSet<>();

    public void setAddress(Address address) {
        if (this.address != null) {
            this.address.setPcsCase(null);
        }
        this.address = address;
        address.setPcsCase(this);
    }

    public void addParty(Party party) {
        parties.add(party);
        party.setPcsCase(this);
    }

    public void addClaim(Claim claim) {
        claims.add(claim);
        claim.setPcsCase(this);
    }

}
