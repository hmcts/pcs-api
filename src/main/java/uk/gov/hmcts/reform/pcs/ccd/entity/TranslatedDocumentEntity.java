package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.TranslatedLanguage;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "translated_document")
public class TranslatedDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "claim_id")
    @JsonBackReference
    private ClaimEntity claim;

    @ManyToOne
    @JoinColumn(name = "counterclaim_id")
    @JsonBackReference
    private CounterClaimEntity counterClaim;

    @ManyToOne
    @JoinColumn(name = "general_application_id")
    @JsonBackReference
    private GenAppEntity genAppEntity;

    @OneToOne(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "document_id")
    private DocumentEntity document;

    @Enumerated(EnumType.STRING)
    private TranslatedLanguage language;

}
