import * as englishData from './page-data';
import * as welshData from './page-data-cy';

const selectedData = process.env.RUN_E2E_IN_WELSH?.toLowerCase() === 'true' ? welshData : englishData;

const data = selectedData as typeof englishData;

export const {
  home
} = data;
