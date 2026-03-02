package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import uk.gov.hmcts.ccd.sdk.api.CCD;

public enum StatementOfTruthCompletedBy {

  @CCD(label = "Claimant") CLAIMANT,

  @CCD(label = "Claimant’s legal representative (as defined by CPR 2.3 (1))")
  LEGAL_REPRESENTATIVE

}
