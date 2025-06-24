import { DateTime } from 'luxon';
import fs from 'fs';
import { userInfo } from 'os';

function convertToSqlDateString(dateTime) {
  if (!dateTime) {
    return 'null';
  }

  return `'${dateTime.toSQLDate()}'`;
}

function isLastElementIndex(elementIndex, mappings) {
  return elementIndex === mappings.length - 1;
}

export const writeMappingsToSql = (mappings, sqlOutputFile) => {
  const lines = [];

  lines.push('BEGIN;');
  lines.push('TRUNCATE postcode_court_mapping;');
  lines.push('INSERT INTO postcode_court_mapping (postcode, epimid, legislative_country, effective_from, effective_to, audit)');
  lines.push('VALUES');

  const currentDateTime = DateTime.utc();

  for (let i = 0; i < mappings.length; i++) {
    const mapping = mappings[i];
    const effectiveFrom = convertToSqlDateString(mapping.effectiveFrom);
    const effectiveTo = convertToSqlDateString(mapping.effectiveTo);
    const auditJson = `{"created_by": "${userInfo().username}", "generated": "${currentDateTime.toISO()}"}`;

    const lineEnding = isLastElementIndex(i, mappings) ? ';' : ','
    lines.push(`  ('${mapping.postCode}', ${mapping.epimId}, '${mapping.legislativeCountry}', `
     + `${effectiveFrom}, ${effectiveTo}, '${auditJson}')${lineEnding}`);
  }

  lines.push('COMMIT;');

  fs.writeFileSync(sqlOutputFile, lines.join('\n'));
  console.log(`SQL statements written to ${sqlOutputFile}`);
}
