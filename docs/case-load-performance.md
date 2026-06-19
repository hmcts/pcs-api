# Case Load Performance

Local benchmark setup:

- AAT dump restored into PostgreSQL 16 at `localhost:55432`
- `pcs-api` running on `http://localhost:3206`
- 42 deterministic case references from the restored AAT data that return `200` locally
- Command: 3 sequential rounds of `GET /ccd-persistence/cases?case-refs=<case-reference>` for every case reference
- Total measured requests per run: 126

## Baseline

Branch point: `master`

| Change | Mean | Median | P90 | P95 | Min | Max |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline | 126.6 ms | 130.9 ms | 153.0 ms | 158.6 ms | 95.0 ms | 176.9 ms |

## Results

| Commit | Change | Mean | Median | P90 | P95 | Min | Max | Improvement vs baseline |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Add case-load indexes | Add concurrent indexes for the case-load read path | 90.8 ms | 100.2 ms | 113.3 ms | 118.4 ms | 66.5 ms | 136.8 ms | 28.3% mean, 23.5% median |
