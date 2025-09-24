import { validateMappings } from './mapping-validator.js';
import { writeMappingsToSql } from './sql-generator.js';
import { parseCsv } from './csv-parser.js';

function logWarnings(warnings) {
  if (warnings.length) {
    console.warn(`\nWARNINGS:\n=========\n`);

    warnings.forEach(warning => {
      console.warn(' - ' + warning);
    })

    console.log('\n');
  }
}

function generateSql() {

  const csvSourceFile = process.argv[2];
  const sqlOutputFile = process.argv[3];

  if (!csvSourceFile || !sqlOutputFile) {
    console.warn(`Usage: node ${process.argv[1]} <csv-file-path> <sql-output-file>`)
    process.exit(1);
  }

  const warnings = [];
  const mappings = parseCsv(csvSourceFile, warnings);
  validateMappings(mappings, warnings);
  writeMappingsToSql(mappings, sqlOutputFile);
  logWarnings(warnings);
}

generateSql();

