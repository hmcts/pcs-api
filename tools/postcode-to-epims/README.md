### Postcode to ePIMS mapping CSV to SQL generator

This script takes a CSV of postcode to ePIMS mappings, performs some validations
and then generates a SQL file with a suitable set of SQL statements to insert
the updated data, (and replace the existing data in the table in the DB).

To set up:

```
yarn install
```

To run:

```
yarn run generate-sql data/sample-source-mappings.csv data/generated.sql
```
