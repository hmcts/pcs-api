package uk.gov.hmcts.reform.pcs.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    private Long reference;

    private String caseDescription;

    @OneToMany
    @JoinColumn(name = "reference", referencedColumnName = "reference")
    @Builder.Default
    private List<Party> parties = new ArrayList<>();

}
