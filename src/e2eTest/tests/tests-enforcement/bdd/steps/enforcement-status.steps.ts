// Enforcement status model (feature 19 and status assertions elsewhere). pcs-api has no enforcement status yet.
import { pendingSteps } from '../fixtures/pending';

pendingSteps({ ticket: 'HDPI-7419', summary: 'Enforcement status model' }, [
  'the case has a warrant of possession with status {string}',
  'a new enforcement of type {string} is added to the case with status {string}',
  'the enforcement status changes to {string}',
  'the enforcement status is still {string}',
  'the case has no live warrant',
  'the enforcement status is {string}',
  'a {string} enforcement is added to the case with status {string}',
  'the case has a warrant with status {string}',
  'the case state is {string}',
  'a warrant moves from {string} to {string}',
  'the case state is still {string}',
  'the {string} event is {string}',
  'the {string}, {string}, {string} and {string} events are available',
  'the enforcement status changes',
]);
