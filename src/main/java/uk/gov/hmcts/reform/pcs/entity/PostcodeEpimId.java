package uk.gov.hmcts.reform.pcs.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
public class PostcodeEpimId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String postcode;

    private int epimid;

}
