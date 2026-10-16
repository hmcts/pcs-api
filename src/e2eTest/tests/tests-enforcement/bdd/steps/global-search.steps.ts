// Find a case by warrant or writ number (feature 16).
import { pendingSteps } from '../fixtures/pending';

pendingSteps({ ticket: 'HDPI-7677', summary: 'Global search by warrant or writ number' }, [
  'a PCS case {string} with warrants {string} and {string}',
  'I search in global search with {string} {string}',
  'the results include case {string}',
  'a new writ {string} is generated on the case',
  'the case\'s global search {string} values include {string}, {string} and {string}',
  'no results are returned',
]);
