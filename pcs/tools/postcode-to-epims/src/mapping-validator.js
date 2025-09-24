import {DateTime} from 'luxon';

const CURRENT_DATE = DateTime.now().startOf('day');
const MAX_DATE = DateTime.fromISO('9999-12-30');
const ONE_DAY = {days: 1};

/*
Perform the following validations on the postcode mappings:

- A postcode can have more than one mapping in the collection but there should be a
  maximum of 1 mapping active at a given time, based on the effective from/to dates

*/
export const validateMappings = (mappings, warnings) => {
  const postCodeIntervals = new Map();

  mappings.forEach(mapping => {
    const postCode = mapping.postCode;
    let intervals = postCodeIntervals.get(postCode);

    if (!intervals) {
      postCodeIntervals.set(postCode, []);
      intervals = postCodeIntervals.get(postCode);
    }

    const effectiveFrom = mapping.effectiveFrom || CURRENT_DATE;
    const effectiveTo = mapping.effectiveTo || MAX_DATE;
    const effectiveInterval = effectiveFrom.until(effectiveTo.plus(ONE_DAY));
    intervals.push(effectiveInterval);
  });

  postCodeIntervals.forEach((effectiveIntervals, postCode) => {
    let interval = effectiveIntervals.shift();

    while (interval) {
      effectiveIntervals.forEach(otherInterval => {
        if (otherInterval.overlaps(interval)) {
          warnings.push(`Overlapping effective dates detected for ${postCode}`);
        }
      });
      interval = effectiveIntervals.shift();
    }
  })

}
