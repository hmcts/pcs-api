package uk.gov.hmcts.reform.pcs.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Placeholder entity for repository integration test.
 */
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "postcode_court_mapping")
public class PostcodeCourtMapping {

    @EmbeddedId
    @Builder.Default
    private PostcodeEpimId id = new PostcodeEpimId();

    private String legislativeCountry;
    private Instant effectiveFrom;
    private Instant effectiveTo;

}
