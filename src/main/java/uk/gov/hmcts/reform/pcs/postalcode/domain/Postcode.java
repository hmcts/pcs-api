package uk.gov.hmcts.reform.pcs.postalcode.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "postcode_lookup")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "epimid")
public class Postcode {

    @Id
    @Column(name = "epimid", nullable = false)
    private int epimid;

    @Column(name = "postcode", length = 20, nullable = false)
    private String postcode;

    @Column(name = "legislativecountry", length = 80, nullable = false)
    private String legislativeCountry;

    @Column(name = "effectivefrom")
    private LocalDate effectiveFrom;

    @Column(name = "effectiveto")
    private LocalDate effectiveTo;

    @Column(name = "audit", nullable = false)
    private String audit;

}
