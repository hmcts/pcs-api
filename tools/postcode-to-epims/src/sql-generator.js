import { DateTime } from 'luxon';
import fs from 'fs';
import nunjucks from 'nunjucks'

const sqlTemplate = `{% macro toSqlDate(dateTime) -%}
    {%- if dateTime -%} '{{ dateTime.toSQLDate() }}' {%- else -%} NULL {%- endif -%}
{%- endmacro %}
BEGIN;
TRUNCATE postcode_court_mapping;
INSERT INTO postcode_court_mapping (postcode, epimid, legislative_country, effective_from, effective_to, audit)
VALUES
{%- for mapping in mappings %}
   ('{{mapping.postCode}}', {{mapping.epimId}}, '{{mapping.legislativeCountry}}', {{ toSqlDate(mapping.effectiveFrom) }}, {{ toSqlDate(mapping.effectiveTo) }}, '{"generated": "{{ generatedAt }}" }')
    {%- if loop.last -%} ; {%- else -%} , {%- endif -%}
{%- endfor %}
COMMIT;`;

export const writeMappingsToSql = (mappings, sqlOutputFile) => {
  const generatedSql = nunjucks.renderString(sqlTemplate, {
    mappings,
    generatedAt: DateTime.utc().toISO()
  });

  fs.writeFileSync(sqlOutputFile, generatedSql);
  console.log(`SQL statements written to ${sqlOutputFile}`);
}
