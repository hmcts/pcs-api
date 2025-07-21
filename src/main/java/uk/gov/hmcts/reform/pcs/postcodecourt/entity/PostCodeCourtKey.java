package uk.gov.hmcts.reform.pcs.postcodecourt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@Embeddable
public class PostCodeCourtKey implements Serializable {

    @Column(name = "postcode", length = 20, nullable = false)
    private String postCode;

    @Column(name = "epimid", nullable = false)
    private Integer epimId;
}
