# pcs-api

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Note: Docker Compose V2 is highly recommended for building and running the application.
In the Compose V2 old `docker-compose` command is replaced with `docker compose`.

Create docker image:

```bash
  docker compose build
```

Run the distribution (created in `build/install/pcs-api` directory)
by executing the following command:

```bash
  docker compose up
```

This will start the API container exposing the application's port `3206`, and a database container running on the port
`5432`.

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:3206/health
```

You should get a response similar to this:

```
  {"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP","details":{"total":62671097856,"free":24688128000,"threshold":10485760,"path":"/opt/app/.","exists":true}},"ping":{"status":"UP"}}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

---

### Running the tests

The Functional tests use [Rest Assured](https://rest-assured.io) and [Serenity](https://serenity-bdd.github.io) for reporting, and are located in the `/src/functionalTest` directory.

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

The E2E UI tests use [Playwright](https://playwright.dev/), and in order to access these you need to cd to the `/src/e2eTests` directory.

This is done with:

```bash
cd src/e2eTests
````

Before running any tests, please install all required packages:

```bash
yarn install
````

The pr suite can be run with the following command:

```bash
yarn test:functional
```

By default, the tests will run against http://localhost:3206/, please update the value on line 3 of `src/e2eTest/config.ts` to change this.

There are also several custom test scripts available:

- `yarn test:changed` - runs only changed spec files
- `test:chrome` - runs the full E2E suite in Chrome
- `test:firefox` - runs the full E2E suite in Firefox

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

