CREATE TABLE postcode_court_mapping (
                               epimid INT PRIMARY KEY,
                               postcode VARCHAR(20) NOT NULL,
                               legislativeCountry VARCHAR(80) DEFAULT 'UK',
                               effectiveFrom DATE DEFAULT CURRENT_DATE,
                               effectiveTo DATE DEFAULT CURRENT_DATE + INTERVAL '1 month',
                               audit TEXT DEFAULT 'system'
);

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (20262, 'W3 7RX', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (36791, 'W3 6RS', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (144641, 'M13 9PL', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (425094, 'LE2 0QB', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (28837, 'UB7 0DG', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');

INSERT INTO postcode_court_mapping (epimid, postcode, legislativeCountry, effectiveFrom, effectiveTo, audit)
VALUES (990000, 'SW1H 9EA', 'UK', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'system');
