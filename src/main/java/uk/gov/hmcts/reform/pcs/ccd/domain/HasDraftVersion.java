package uk.gov.hmcts.reform.pcs.ccd.domain;

/**
 * A draft journey's root answers object that carries the version of the stored draft it was read
 * from, so a later save or submit can be bound to it.
 */
public interface HasDraftVersion {

    Long getDraftVersion();

    void setDraftVersion(Long draftVersion);

}
