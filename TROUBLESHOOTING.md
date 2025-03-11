# Troubleshooting

This is guide to troubleshooting common issues with the application.

Peter Pilgrim
11 March 2025



## PCS API Authentication Authorisation Failure

Error: `unauthorized` when starting the pcs-api service locally.


```zsh
docker compose up
[+] Running 2/2
 ✘ service-auth-provider-api Error Head "https://hmctspublic.azurecr.io/v2/rpe/service-auth-provider/manifests/latest": unauthorized:                                                                                                          0.2s
 ✘ pcs-api-db Error                context canceled                                                                                                                                                                                            0.2s
Error response from daemon: Head "https://hmctspublic.azurecr.io/v2/rpe/service-auth-provider/manifests/latest": unauthorized:
```

Resolution:

Step 1)
Ensure you are logged into Azure at the command line.

```zsh
az login
```

Hint: You might have to restart IntelliJ IDEA or Visual Studio Code in order to pick up the new Azure CLI session.


Step 2)
Ensure that you are logged into the Docker Hub with the correct credentials.

```zsh
docker login -u <docker-username>  [ -p <docker-password> ]
```

Substitute with with your own Docker Hub account credentials. Docker Hub is a public registry [https://hub.docker.com/](https://hub.docker.com/).


Step 3)
Login to the Azure Container Registry (ACR) with the correct credentials.

```zsh
az acr login --name hmctspublic --subscription DCD-CNP-DEV
```

Step 4)
Resolution in this case is to logout if you logged to ACR before or login every time you want to run a docker command.

```zsh
docker logout hmctspublic.azurecr.io
```

Step 5)
Afterwards the invoke the command to start the application locally

```zsh
docker compose up
```

The console output should be similar to this:

```
service-auth-provider-api-1  | Picked up JAVA_TOOL_OPTIONS: -XX:InitialRAMPercentage=30.0 -XX:MaxRAMPercentage=65.0 -XX:MinRAMPercentage=30.0  -javaagent:/opt/app/applicationinsights-agent-3.4.18.jar -Dfile.encoding=UTF-8
pcs-api-db-1                 | The files belonging to this database system will be owned by user "postgres".
pcs-api-db-1                 | This user must also own the server process.

...

pcs-api-1                    | 2025-03-05T15:00:15.516 INFO  [main] uk.gov.hmcts.reform.pcs.Application Starting Application v0b06573 using Java 21.0.5 with PID 1 (/opt/app/pcs-api.jar started by hmcts in /opt/app)
pcs-api-1                    | 2025-03-05T15:00:15.516 INFO  [main] uk.gov.hmcts.reform.pcs.Application No active profile set, falling back to 1 default profile: "default"
pcs-api-1                    | 2025-03-05T15:00:16.196 INFO  [main] o.springframework.cloud.context.scope.GenericScope BeanFactory id=fb33409b-b939-3330-a52b-c5e899e27566
pcs-api-1                    | 2025-03-05T15:00:16.447 INFO  [main] o.s.boot.web.embedded.tomcat.TomcatWebServer Tomcat initialized with port 3206 (http)
pcs-api-1                    | 2025-03-05T15:00:16.454 INFO  [main] org.apache.coyote.http11.Http11NioProtocol Initializing ProtocolHandler ["http-nio-3206"]
pcs-api-1                    | 2025-03-05T15:00:16.454 I
```

Application is available locally at port 3206 http://localhost:3206/health


## Check Your Current CNP Subscription

Check that you in the correct Cloud Native Platform (CNP) Subscription with the Azure CLI.
At the terminal running `az account show` tells you that you are a part of DCD-CNP-DEV?

```zsh
az account show
    ...
    "name": "DCD-CNP-DEV",
      "state": "Enabled",
      "tenantDefaultDomain": "HMCTS.NET",
      "tenantDisplayName": "CJS Common Platform",
      "tenantId": "XXXXXXXX-xxxx-xxxx-XXXXXXXXXXXXXXXXX",
      "user": {
        "name": "Jim.Beam@HMCTS.NET",
        "type": "user"
      }
```

Here are the steps to switch to the correct subscription:

```zsh
az account list
az account set --subscription DCD-CNP-DEV
```

