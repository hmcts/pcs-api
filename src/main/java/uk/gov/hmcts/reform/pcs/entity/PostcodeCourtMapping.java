package uk.gov.hmcts.reform.pcs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "postcode_court_mapping")
@Getter
@Setter
public class PostcodeCourtMapping {

    @Id
    @Column(name = "postcode")
    private String postcode;

    @Column(name = "epimid")
    private int epimid;

}
