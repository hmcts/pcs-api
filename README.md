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
|-----------------------------------|------|
| PCS-API                           | 3206 |
| XUI Manage case (UI Access point) | 3000 |
| Elastic search                    | 9200 |
| Postgres Database                 | 6432 |
| IDAM Simulator                    | 5062 |
| XUI Manage org                    | 3001 |

Once successfully loaded open XUI at http://localhost:3000
See CftlibConfig.java for users and login details.

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

Then set the following environment variables based on the value below or named secret
from the PCS AAT key vault:

| Environment Variable     | Value or Secret Name                                             |
|--------------------------|------------------------------------------------------------------|
| LOCATION_REF_URL         | http://rd-location-ref-api-aat.service.core-compute-aat.internal |
| PCS_API_S2S_SECRET       | secret: pcs-api-s2s-secret                                       |
| IDAM_CLIENT_SECRET       | secret: pcs-api-idam-secret                                      |
| PCS_IDAM_SYSTEM_USERNAME | secret: idam-system-user-name                                    |
| PCS_IDAM_SYSTEM_PASSWORD | secret: idam-system-user-password                                |


Finally, run the service with the `bootWithCCD` task as above.

In order to test if the API is healthy, you can call its health endpoint:

```bash
  curl http://localhost:3206/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"coreCaseData":{"status":"UP"},"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"dbScheduler":{"status":"UP","details":{"state":"started"}},"discoveryComposite":{"description":"Discovery Client not initialized","status":"UNKNOWN","components":{"discoveryClient":{"description":"Discovery Client not initialized","status":"UNKNOWN"}}},"diskSpace":{"status":"UP","details":{"total":494384795648,"free":167924658176,"threshold":10485760,"path":"/Users/jakegowler/Documents/HMCTS/pcs/pcs-api/.","exists":true}},"ping":{"status":"UP"},"refreshScope":{"status":"UP"},"serviceAuth":{"status":"UP"},"ssl":{"status":"UP","details":{"validChains":[],"invalidChains":[]}}}}
```

To access the swagger documentation for the API, go to http://localhost:3206/swagger-ui/index.html

#### Document Management

- To get document upload working locally, it's easier to connect to AAT dependencies using a VPN.
- Set the authentication mode to `AuthMode.AAT` as described above.

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

To run the tests, use:
```bash
./gradlew functional
````

To run tests based on tags, use the following command (replace `tagName` with the desired tag):
```bash
./gradlew functional -Dtags="tagName"
````

Additionally, you can configure the tags to run in the pipeline by editing the functional task in the `build.gradle`, line:

`includeTags System.getProperty("tags", "Functional")`

After the tests run, the report will be available under the /[report-for-functional-tests](report-for-functional-tests) folder, in a file named `index.html`.

---

The E2E UI tests use [Playwright](https://playwright.dev/), and in order to access these you need to cd to the `/src/e2eTest` directory.

This is done with:

```bash
cd src/e2eTest
````

Before running any tests, please install all required packages:

```bash
yarn install
````
Running the tests:

The e2e tests use playwright, and are located in the /src/e2eTest directory.

The following environment variables are needed to run the tests:

- IDAM_SYSTEM_USERNAME
- IDAM_SYSTEM_USER_PASSWORD
- PCS_IDAM_TEST_USER_PASSWORD
- PCS_API_IDAM_SECRET
- MANAGE_CASE_BASE_URL
- CHANGE_ID (same as PR number - Required only pointing to Preview env)

The e2e suite can be run with the following command:

```bash
yarn test:chrome
```
There are also several custom test scripts available:

- `yarn test:changed` - runs only changed spec files
- `yarn test:chrome` - runs the full E2E suite in Chrome

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

