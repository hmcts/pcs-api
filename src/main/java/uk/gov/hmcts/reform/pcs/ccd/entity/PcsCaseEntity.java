package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "pcs_case")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(
    name = "PcsCaseEntity.parties",
    attributeNodes = {
        @NamedAttributeNode("parties")
    }
)
public class PcsCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    @OneToOne(cascade = ALL,orphanRemoval = true)
    private AddressEntity propertyAddress;

    private Integer caseManagementLocation;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<PartyEntity> parties = new HashSet<>();

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<ClaimEntity> claims = new HashSet<>();


    public void addParty(PartyEntity party) {
        parties.add(party);
        party.setPcsCase(this);
    }

    @OneToMany(mappedBy = "pcsCase", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private Set<DocumentEntity> documents = new HashSet<>();

    public void addDocument(DocumentEntity document) {
        documents.add(document);
        document.setPcsCase(this);
    }
}
