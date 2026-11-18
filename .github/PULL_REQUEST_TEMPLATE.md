### Jira link

<!--
Replace HDPI-XXXXXX with your Jira key
Remove this section if its not applicable, or replace it with another reference link
-->
See [HDPI-XXXXXX](https://hmcts.atlassian.net/browse/HDPI-XXXXXX)

### Change description

<!--
Provide a description of what change you are proposing.
A short summary here and then you can add comments in your pull request explaining your change to others.
-->

### Testing done

<!-- Comment:
Provide a clear description of how this change was tested.
At minimum this should include proof that a computer has executed the changed lines.
Ideally this should include an automated test or an explanation as to why this change has no tests.
Note that automated test coverage is not always complete, so a successful pull request build does not necessarily imply that a computer has executed the changed lines.
If automated test coverage does not exist for the lines you are changing, you must describe the scenario(s) in which you manually tested the change.
For frontend changes, include screenshots of the relevant page(s) before and after the change.
For refactoring and code cleanup changes, exercise the code before and after the change and verify the behaviour remains the same.
-->

### Security Vulnerability Assessment ###

<!-- Comment:
If Yes to the below question, please provide details below:
CVE ID(s): (List all suppressed or relevant CVE IDs)
Reason for Suppression/Ignoring: (e.g., Low risk in our specific context, Mitigating controls in place, False positive - with justification)
Mitigating Factors/Compensating Controls: Describe any measures taken to reduce the risk associated with the vulnerability
-->

**CVE Suppression:** Are there any CVEs present in the codebase (either newly introduced or pre-existing) that are being intentionally suppressed or ignored by this commit?
  * [ ] Yes
  * [ ] No

### Feature flag assessment

<!-- Comment:
If Yes, name the flag(s) (FeatureFlag enum) and what users see with the flag OFF (normally "no change").
If No, give a short reason (e.g. infra-only, already gated, no user-facing behaviour).
A CCD showCondition only hides the UI: the event's start, mid-event and submit handlers must also check featureToggleService.
-->

**Feature flag:** Does this PR introduce or change behaviour that should be behind feature flag(s)?

- [ ] Yes — flag(s):
- [ ] No — reason:

**If Yes:**

- [ ] What users see with the flag OFF is stated above (normally "no change")
- [ ] Tested with the flag OFF: existing events, page labels, show conditions, states and tabs are unchanged
- [ ] Server-side check (`featureToggleService.isEnabled`) in the start, mid-event and submit handlers, not only a CCD showCondition
- [ ] No unflagged changes to live data (migrations, backfills, scheduled jobs, notifications, WA tasks)
- [ ] Unit tests cover both flag states

### Checklist

<!-- Check each box by removing the space and adding an x, e.g. [x] -->

- [ ] commit messages are meaningful and follow good commit message guidelines
- [ ] README and other documentation has been updated / added (if needed)
- [ ] tests have been updated / new tests has been added (if needed)
- [ ] Does this PR introduce a breaking change

### PCS checklist

- [ ] New functional test classes carry the gating annotations (`@EnabledIfEnvironmentVariable(named = "CCD_ENABLED", ...)` + shutter guard) unless they must run on unlabelled PRs (HDPI-8084)
- [ ] Infrastructure / chart / suppression changes have a second pair of eyes from the service admins (HDPI-8153)
