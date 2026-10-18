import { statementOfTruth } from './statementOfTruth.page.data';

/** The writ's statement of truth: the warrant page's content plus the certification it lists. */
export const statementOfTruthWrit = {
  ...statementOfTruth,
  detailsGivenAreCorrectListItem: `The details I have given are correct and that to my knowledge there is no application or other procedure pending.`,
  requestAnOrderOfEnforcementListItem: `I request an order for enforcement in the High Court by Writ of Possession.`,
  factsStatedAreTrueListItem: `I believe that the facts stated in this form are true.`,
};
