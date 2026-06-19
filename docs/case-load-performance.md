# Case Load Performance

Local benchmark setup:

- AAT dump restored into PostgreSQL 16 at `localhost:55432`
- `pcs-api` running on `http://localhost:3206`
- 42 deterministic case references from the restored AAT data that return `200` locally
- Command: sequential rounds of `GET /ccd-persistence/cases?case-refs=<case-reference>` for every case reference
- Total measured requests per run: 126 for the initial runs; 2,100 for the longer profile-guided run.

## Baseline

Branch point: `master`

| Change | Requests | Mean | Median | P90 | P95 | Min | Max |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline | 126 | 126.6 ms | 130.9 ms | 153.0 ms | 158.6 ms | 95.0 ms | 176.9 ms |

## Results

| Commit | Change | Requests | Mean | Median | P90 | P95 | Min | Max | Improvement vs previous | Improvement vs baseline |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Add case-load indexes | Add concurrent indexes for the case-load read path | 126 | 90.8 ms | 100.2 ms | 113.3 ms | 118.4 ms | 66.5 ms | 136.8 ms | 28.3% mean, 23.5% median | 28.3% mean, 23.5% median |
| Avoid repeated claim-party graph loading | Make `ClaimPartyEntity` parent links lazy and reuse loaded claim-party roles when mapping party flags | 126 | 36.7 ms | 35.4 ms | 41.3 ms | 47.6 ms | 31.9 ms | 56.5 ms | 59.6% mean, 64.7% median | 71.0% mean, 73.0% median |
| Add claim projection indexes | Add concurrent indexes for the claim-adjacent joins dominating the latest profile | 126 | 12.5 ms | 11.9 ms | 14.1 ms | 14.6 ms | 9.1 ms | 55.9 ms | 65.9% mean, 66.4% median | 90.1% mean, 90.9% median |
| Batch party collection fetches with subselects | Use `SUBSELECT` fetch mode for party legal representatives and defendant flags | 2,100 | 9.3 ms | 9.2 ms | 10.3 ms | 10.7 ms | 6.9 ms | 25.1 ms | 25.4% mean, 22.4% median | 92.6% mean, 92.9% median |
