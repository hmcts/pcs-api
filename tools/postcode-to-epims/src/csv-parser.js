import fs from 'fs';
import {parse} from 'csv-parse/sync';
import { DateTime } from 'luxon';

function convertToDateTime(sourceDateString) {
  if (!sourceDateString || sourceDateString === 'null') {
    return null;
  }

  return DateTime.fromFormat(sourceDateString, 'dd/MM/yyyy');
}

export const readCsvData = (csvSourceFile) => {
  let csvData;
  try {
    csvData = fs.readFileSync(csvSourceFile);
  } catch (e) {
    console.warn(`Unable to read file at ${csvSourceFile}`)
    process.exit(2);
  }

  const rows = parse(csvData, {bom: true});

  if (rows.length < 2) {
    console.warn('No data rows found in CSV');
    process.exit(3);
  }

  // Remove the header row
  rows.shift();
  return rows;
}

function convertRowsToMappings(rows, warnings) {
  let warningRowNumber = 2; // Row 1 is the header row in the sourcr CSV
  return rows.map(row => {
    const mapping = {};

    let sourceEffectiveFrom;
    let sourceEffectiveTo;
    [mapping.postCode, mapping.epimId, mapping.legislativeCountry, sourceEffectiveFrom, sourceEffectiveTo] = row;

    mapping.effectiveFrom = convertToDateTime(sourceEffectiveFrom);
    if (!mapping.effectiveFrom) {
      warnings.push(`Row ${warningRowNumber}: Missing mandatory Effective From date. Row will be omitted.`);
      mapping.excludeFromSql = true;
    } else if (!mapping.effectiveFrom.isValid) {
      mapping.excludeFromSql = true;
      warnings.push(`Row ${warningRowNumber}: Invalid Effective From date: ${sourceEffectiveFrom}. Row will be omitted.`);
    }

    mapping.effectiveTo = convertToDateTime(sourceEffectiveTo);
    if (mapping.effectiveTo && !mapping.effectiveTo.isValid) {
      warnings.push(`Row ${warningRowNumber}: Invalid Effective To date ${sourceEffectiveTo}`);
    }

    if (mapping.effectiveFrom && mapping.effectiveTo && mapping.effectiveFrom > mapping.effectiveTo) {
      warnings.push(`Row ${warningRowNumber}: Effective From date ${sourceEffectiveFrom} is after Effective To date ${sourceEffectiveTo}`);
    }

    if (mapping.effectiveTo && mapping.effectiveTo < DateTime.now().startOf('day')) {
      mapping.excludeFromSql = true;
      warnings.push(`Mapping for ${mapping.postCode} expired on ${mapping.effectiveTo.toLocaleString()}. Row will be omitted`);
    }

    warningRowNumber++;
    return mapping;
  });
}

export const parseCsv = (csvSourceFile, warnings) => {
  const rows = readCsvData(csvSourceFile);
  return convertRowsToMappings(rows, warnings);
}
