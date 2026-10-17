# pcs-api

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running pcs-api with local CCD

```bash
./gradlew bootWithCCD
```

Above command starts PCS API + CCD & all dependencies

This will start several containers:

| Container                         | Port |
| --------------------------------- | ---- |
| PCS-API                           | 3206 |
| XUI Manage case (UI Access point) | 3000 |
| Elastic search                    | 9200 |
| Postgres Database                 | 6432 |
| IDAM Simulator                    | 5062 |
| XUI Manage org                    | 3001 |

Once successfully loaded open XUI at http://localhost:3000
See CftlibConfig.java for users and login details.

#### Local environment variables

pcs-api reads configuration from the **process environment** (and Spring system properties). Values are wired in
`src/main/resources/application.yaml` using the usual `${VAR_NAME:default}` pattern.

A root `.env` file is **not** loaded automatically by Spring or by `./gradlew bootWithCCD`. To override local
defaults, set variables in the same terminal session (or IDE run configuration) before starting the app, for
example:

```bash
export SOME_VAR=value
./gradlew bootWithCCD
```

**Only export the variables you intend to override.** A local `.env` can be a handy place to
keep candidate values, but pcs-api will not read that file. If you export every variable
from it into the shell (or IDE run config) before `bootWithCCD`, values such as
`SERVER_PORT` can conflict with the managed local stack.

By default, this runs with local instance of IDAM and
S2S services. However sometimes it may be required to run
with the AAT instances of those services, (for example when running both pcs-frontend and pcs-api locally).

To do this, edit the `build.gradle` file before running the `bootWithCCD` task and replace

```
authMode = AuthMode.Local
```

with

```
authMode = AuthMode.AAT
```

Then set the following environment variables (as above) based on the value below or named secret
from the PCS AAT key vault:

| Environment Variable         | Value or Secret Name                                                         |
| ---------------------------- | ---------------------------------------------------------------------------- |
| LOCATION_REF_URL             | http://rd-location-ref-api-aat.service.core-compute-aat.internal             |
| DATA_STORE_URL_BASE          | https://ccd-data-store-api-pcs-api-pr-{CHANGE_ID}.preview.platform.hmcts.net |
| PCS_API_S2S_SECRET           | secret: pcs-api-s2s-secret                                                   |
| IDAM_CLIENT_SECRET           | secret: pcs-api-idam-secret                                                  |
| PCS_IDAM_SYSTEM_USERNAME     | secret: idam-system-user-name                                                |
| PCS_IDAM_SYSTEM_PASSWORD     | secret: idam-system-user-password                                            |
| PCS_PRD_ADMIN_USERNAME       | secret: pcs-prd-admin-username                                               |
| PCS_PRD_ADMIN_PASSWORD       | secret: pcs-prd-admin-password                                               |
| IDAM_PCS_USER_PASSWORD       | secret: idam-pcs-user-password                                               |
| PCS_SOLICITOR_AUTOMATION_UID | secret: pcs-solicitor-automation-uid                                         |

Finally, run the service with the `bootWithCCD` task as above.

In order to test if the API is healthy, you can call its health endpoint:

```bash
  curl http://localhost:3206/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"coreCaseData":{"status":"UP"},"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"dbScheduler":{"status":"UP","details":{"state":"started"}},"discoveryComposite":{"description":"Discovery Client not initialized","status":"UNKNOWN","components":{"discoveryClient":{"description":"Discovery Client not initialized","status":"UNKNOWN"}}},"diskSpace":{"status":"UP","details":{"total":494384795648,"free":167924658176,"threshold":10485760,"path":"/Users/jakegowler/Documents/HMCTS/pcs/pcs-api/.","exists":true}},"ping":{"status":"UP"},"refreshScope":{"status":"UP"},"serviceAuth":{"status":"UP"},"ssl":{"status":"UP","details":{"validChains":[],"invalidChains":[]}}}}
```

To access the swagger documentation for the API, go to http://localhost:3206/swagger-ui/index.html.

#### Document Management

To get document upload working locally, set the environment variable `LOCAL_DOC_SERVICES=true` in your Run config or before
running `bootWithCCD` in a shell. This will start a docker container with dm-store and its dependencies and
also override the necessary environment variables in `build.gradle` so that CDAM and ExUI will talk
to the local dm-store.

The first time this is run locally there will be a small additional delay at startup while the dm-store container
spins up. However, it will remain up even when `bootWithCCD` is stopped so restarting pcs-api will not incur that
additional delay each time.

In order for document generation and viewing of documents in Case File View to work, it is currently necessary
to be on the VPN as the AAT version of Docmosis is still used. (This does not need `AuthMode.AAT`).

NOTE: This is a new configuration for running the dm-store locally. If it proves to be practical for developers
then it may be updated to alwys run when `AuthMode.Local` is set.

If the above fails, then it may be easier to connect
to AAT dependencies using a VPN, by setting the authentication mode to `AuthMode.AAT` as described above and ensuring
that `LOCAL_DOC_SERVICES` is not set to `true`.


#### Generating the CCD definition XLS

We can generate the xlsx spreadsheet by running the script:

```
./bin/create-xlsx.sh
```

This spreadsheet can be found in: /build/definitions/CCD_Definition_PCS_ENVIRONMENT.xlsx

You can specify which environment to generate the spreadsheet for by passing it in when calling the script. E.g ./bin/create-xlsx.sh preview.

If you encounter the error:

```
 [{"code":"UNAUTHORIZED","message":"authentication required,}]
```

Run:

```
az login
```

```
- az acr login -n hmctsprod
```

### Enabling work allocation for local Dev

To enable work allocation for local dev, run:

```
docker compose -f docker-compose-wa.yml up -d
```

You may need to authenticate to the `hmctssandbox` and `hmctsprod` ACR repos first:

```
az acr login -n hmctssandbox
az acr login -n hmctsprod
```

This will start a local Camunda instance, wa-workflow-api and wa-task-monitor.

The Camunda container only uses an in-memory H2 DB for simplicity so any changes are lost on restart.
However it will automatically load any DMNs in /src/main/resources/dmn at startup so will reset to
whatever is in the local workspace. If you make changes to the DMNs and wish to update them in
Camunda without restarting the container then run this script to re-import them:

```
cd /bin/wa
./local-import-dmn-diagrams.sh
```

To trigger the task
monitor to check for and configure any unconfigured Camunda tasks, make this request in your Rest
client or with curl:

```
POST http://localhost:8077/monitor/tasks/jobs
Headers:
  ServiceAuthorization:Bearer {{s2s token for 'pcs_api'}}

Payload:

{
    "job_details": {
        "name": "INITIATION"
    }
}
```

Note that there seems to be a delay of a couple of seconds before a freshly submitted Camunda task
is detected by the task monitor, so you may need to trigger the poll a second time if no Camunda tasks were picked
up and configured as WA tasks.

#### How PCS creates tasks

pcs-api does not publish CCD case events to Service Bus, so `wa-case-event-handler` never sees a PCS event.
Instead `CamundaService` posts a `createTaskMessage` message straight to `wa-workflow-api`, carrying the
`taskId`, `name` and `__processCategory__<taskId>` process variables taken from the `TaskType` enum, and a
`cancelTasks` message to cancel. That has a consequence for the six DMN tables in
`src/main/resources/dmn`:

| Table | Evaluated by | Does it affect PCS today? |
|---|---|---|
| `wa-task-configuration` | `wa-task-management-api` | Yes — sets every task attribute |
| `wa-task-permissions` | `wa-task-management-api` | Yes — sets who can see and work the task |
| `wa-task-types` | `wa-task-management-api` | Yes — XUI's task type filters |
| `wa-task-completion` | `wa-task-management-api` (`/task/search-for-completable`) | Yes — closes a task when its event is submitted |
| `wa-task-initiation` | `wa-case-event-handler` | No — nothing publishes the case events it reads |
| `wa-task-cancellation` | `wa-case-event-handler` | No — the same |

So a new task type needs rows in the configuration, permissions, types and completion tables to work. Keep the
initiation and cancellation rows correct alongside them: they are what will fire if pcs-api starts publishing
case events, and until then they are the specification the `CamundaService` calls have to match.

#### Enforcement tasks

| Task | Raised when | Due | Closed by | Roles |
|---|---|---|---|---|
| Review claimant warrant application | the application reaches `AWAITING_REVIEW` | 10 working days | `decideEnforcement` | `hearing-centre-admin`, `hearing-centre-team-leader` |
| Allocate case to bailiff | a warrant reaches `AWAITING_BAILIFF_ALLOCATION`, on approval or on a "Reopen warrant" return | 5 working days | `allocateBailiff` | `bailiff-admin`, `bailiff-manager` |
| Review risks and set level | a bailiff is allocated | 5 working days | `setRiskLevel` | `allocated-bailiff`, `bailiff-manager` |
| Mark notice of eviction as served | the eviction is scheduled or rescheduled and the court has not dispensed with service | 5 working days | `markNoticeOfEvictionServed` | `allocated-bailiff`, `bailiff-manager` |
| Reschedule eviction | the claimant answers that they cannot attend | 5 working days | `rescheduleEviction` | `allocated-bailiff`, `bailiff-manager` |
| Review updated risk information | the claimant changes a risk assessment answer | 5 working days | `setRiskLevel` | `allocated-bailiff`, `bailiff-manager` |
| Record warrant return | the scheduled eviction date arrives | 5 working days | `recordWarrantReturn` | `allocated-bailiff`, `bailiff-manager` |

A return that closes the enforcement cancels whichever of these are still open.

Only the review task's 10 working days and its major priority of 2000, band 2 of the 7 the configuration table
uses, come from the requirements (HDPI-9539). The other tasks have no stated SLA or priority, so they take 5
working days and 5000, which is what the rest of PCS uses.

Enforcement status is held on the enforcement record and never changes the case state, so the initiation and
cancellation rules read it out of case data (`enforcementStatus`) rather than out of `postEventState`. Three
tasks have no initiation rule, because no CCD event raises them:

- **Review claimant warrant application** on a paid application. Paying the fee is not a CCD event:
  `EnforcementOrderService#recordFeePaid` moves the status, so that path has to call `CamundaService`
  (HDPI-2877). The initiation row covers only the fee-free restitution applications, which reach
  `AWAITING_REVIEW` on `enforceTheOrder`.
- **Review updated risk information**. No case data field records that the claimant changed a risk assessment
  answer (HDPI-7552), so there is nothing a rule could test.
- **Record warrant return**, which is raised on the eviction date by a scheduled job rather than by an event.

`allocated-bailiff` is the case role `AllocateBailiff` grants, which is how the bailiff tasks auto-assign to the
allocated bailiff and follow a reallocation. Granting it needs `am-role-assignment-service`, which the preview
environment does not currently deploy.

#### Editing a DMN table

`WaDmnTablesTest` and `EnforcementWaDmnTest` evaluate the tables from the classpath and assert the rows a given
input collects, so an edit that drops a cell, misspells a task type or leaves a task type without permissions or
a work type fails `./gradlew test`. To see the tables run for real, start `camunda-local` from
`docker-compose-wa.yml` and post to
`/engine-rest/decision-definition/key/<decision-id>/tenant-id/pcs/evaluate`.

Keep every condition a FEEL unary test, which means a string literal or a comma-separated list of them. Camunda
evaluates every rule's cell for a column it reads, so one cell holding free text fails the whole decision for
every input. The initiation table came with 37 such cells spread over 23 of its 24 rules — a real engine throws
`FEEL/SCALA-01008 ... failed to parse expression 'Tech team update case state'` on it for any event — which
does no harm only because nothing publishes PCS case events. They have to go before that changes.

### Running the tests

The Functional tests use [Rest Assured](https://rest-assured.io) and [Serenity](https://serenity-bdd.github.io) for reporting, and are located in the `/src/functionalTest` directory.

The following environment variables are needed to run the tests:

- PCS_API_S2S_SECRET
- TEST_URL
- IDAM_S2S_AUTH_URL
- IDAM_API_URL
- IDAM_SYSTEM_USERNAME
- IDAM_SYSTEM_USER_PASSWORD
- PCS_API_IDAM_SECRET
- IDAM_EXPIRED_USER_TOKEN
- S2S_EXPIRED_TOKEN
- IDAM_TESTING_SUPPORT_URL

To run the tests, use:

```bash
./gradlew functional
```

To run tests based on tags, use the following command (replace `tagName` with the desired tag):

```bash
./gradlew functional -Dtags="tagName"
```

Additionally, you can configure the tags to run in the pipeline by editing the functional task in the `build.gradle`, line:

`includeTags System.getProperty("tags", "Functional")`

After the tests run, the report will be available under the /[report-for-functional-tests](report-for-functional-tests) folder, in a file named `index.html`.

---

The E2E UI tests use [Playwright](https://playwright.dev/), and in order to access these you need to cd to the `/src/e2eTest` directory.

This is done with:

```bash
cd src/e2eTest
```

Before running any tests, please install all required packages:

```bash
yarn install
```

Running the tests:

The e2e tests use playwright, and are located in the /src/e2eTest directory.

The following environment variables are needed to run the tests:

- PCS_API_IDAM_SECRET
- IDAM_PCS_USER_PASSWORD
- DATA_STORE_URL_BASE
- MANAGE_CASE_BASE_URL
- CHANGE_ID (same as PR number - Required only pointing to Preview env)

The e2e suite can be run with the following command:

To run the critical tests/PR tagged tests, use:

```bash
yarn test:pr
```

There are also several custom test scripts available:

- `yarn test:regression` - runs regression tagged tests
- `yarn test:chrome` - runs the full E2E suite in Chrome
- `yarn test:firefox` - runs the full E2E suite in Firefox
- `yarn test:webkit` - runs the full E2E suite in WebKit (Safari engine)
- `yarn test:edge` - runs the full E2E suite in Microsoft Edge
- `yarn test:mobile-android` / `yarn test:mobile-ios` / `yarn test:mobile-ipad` - run the full suite on the matching Playwright device profile

Locally, `playwright.config.ts` registers Firefox, WebKit, Edge, and mobile projects only when `CI` is set (as on Jenkins). For a local multi-browser run, set `CI=true` or pass `--project <name>` for a project that is defined in config.

The nightly Jenkins job (`Jenkinsfile_nightly`) runs against **AAT** and offers `PLAYWRIGHT_GREP_TAG` (choice) and `PLAYWRIGHT_SPEC` (text). `handleEnvironmentSetting()` sets `E2E_TEST_SCOPE` and `E2E_SPEC` on the run; each matrix stage sets only `E2E_SUITE` in `withEnv` before `./gradlew runE2eTests` → `yarn test:<suite>`. `src/e2eTest/playwright.config.ts` reads those env vars for grep and `testMatch`.

To open generated Allure report

```bash
yarn test:openAllureReport
```

Permanent IDAM Users:
All permanent users needs to be added to ./data/permanent-users.data
Temporary IDAM Users:
During test execution, temporary users are automatically created and tracked in a file ./data/.temp-users.data.json
Update ./config/global-setup.config with list of roles for which temporary users needs to be created along with the key/name to
identify them.

### Running with Wiremock in local environment

The docker-compose-wiremock.yml in the project root will start wiremock with mappings
from the wiremock/mappings folder. Currently this has mappings to simulate the payment API
callback and also to provide a fixed professional ref data response for an organisation.

It can be run as follows:

```
docker compose -f docker-compose-wiremock.yml up -d
```

### Running with Wiremock in Preview environment

To enable a Wiremock pod in the Preview environment for a PR, add the label `pr-values:wiremock` to the PR. This
will deploy a Wiremock instance pre-configured to respond to health/liveness checks, and proxy any requests
for certain APIs to their AAT origin.

Those API requests can then have mappings added for them to change the response. For example to update the Fee Register
response for the possession claim fee:

**POST** https://wiremock-pcs-api-pr-NNNN.preview.platform.hmcts.net/__admin/mappings

**Headers**: Content-Type:application/json

**Body**:

```json
{
  "request": {
    "method": "GET",
    "urlPath": "/fees-register/fees/lookup",
    "queryParameters": {
      "service": {
        "equalTo": "possession claim"
      },
      "jurisdiction1": {
        "equalTo": "civil"
      },
      "jurisdiction2": {
        "equalTo": "county court"
      },
      "channel": {
        "equalTo": "default"
      },
      "event": {
        "equalTo": "issue"
      },
      "amount_or_volume": {
        "equalTo": "1"
      },
      "keyword": {
        "equalTo": "PossessionCC"
      }
    }
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "code": "FEE0412",
      "description": "Recovery of Land - County Court (Test)",
      "version": 1,
      "fee_amount": 1234.56
    },
    "headers": {
      "Content-Type": "application/json"
    }
  }
}
```

## Feature flags

We use [LaunchDarkly](https://launchdarkly.com) for feature flags, so behaviour can be turned on or
off per environment without a redeploy. Flags live in the `FeatureFlag` enum and are read through
`FeatureToggleService`:

```java
if (featureToggle.isEnabled(FeatureFlag.BULK_PRINT)) {
    // ...
}
```

Each flag has a key matching the LaunchDarkly dashboard and a default served when LaunchDarkly can't
be reached. The first flag is `bulk-print-enabled`. To add one, add a constant
(`MY_FEATURE("my-feature-enabled", false)`) and create the matching flag in LaunchDarkly; to retire
one, delete the constant and the compiler points you at every use.

The SDK key comes from the key vault (`LAUNCHDARKLY_SDK_KEY`) and `LAUNCHDARKLY_ENV` sets the
environment used for targeting.

### Local Dev

When running the pcs-api locally, set `LAUNCHDARKLY_OFFLINE` to `true` and optionally specify one or more files
containing flags values to be used. This is set up by default for cftLibTest and bootWithCcd

With no key the client runs offline and every flag uses its default,
so it still runs locally; local and `cftlibTest` set `LAUNCHDARKLY_OFFLINE=true`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
