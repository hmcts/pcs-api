### Checklist
- [ ] New functional test classes carry the gating annotations (`@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", ...)` + shutter guard) unless they must run on unlabelled PRs (HDPI-8084)
