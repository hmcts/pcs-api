package uk.gov.hmcts.reform.pcs.ccd.entity;


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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PCS {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long ccdCaseReference;

    @OneToOne(mappedBy = "pcsCase", cascade = ALL)
    private Address propertyAddress;

    @OneToMany(mappedBy = "pcsCase", cascade = ALL, orphanRemoval = true)
    @Builder.Default
    private List<GenApplication> generalApplications = new ArrayList<>();
}
