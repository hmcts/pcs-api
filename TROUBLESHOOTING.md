# Troubleshooting

This is guide to troubleshooting common issues with the application.

Peter Pilgrim
11 March 2025



## PCS API Authentication Failure

Here are my notes when I received the following authentication error: *unauthorized* when I tried to start the `pcs-api` service.
I was trying to start the `pcs-api` service with `docker compose up` and I received the following error message:

```zsh
docker compose up
[+] Running 2/2
 ✘ service-auth-provider-api Error Head "https://hmctspublic.azurecr.io/v2/rpe/service-auth-provider/manifests/latest": unauthorized:                                                                                                          0.2s
 ✘ pcs-api-db Error                context canceled                                                                                                                                                                                            0.2s
Error response from daemon: Head "https://hmctspublic.azurecr.io/v2/rpe/service-auth-provider/manifests/latest": unauthorized:
```

Resolution to server side `pcs-api` issue. I had to log into the Azure Container Registry (ACR) with the correct credentials.
I had to log out of the ACR and then log back in with the correct credentials.

Login to Docker / Get a Docker Hub account

```zsh
docker login -u peterpilgrim
```

Substitute with with your own Docker Hub account credentials. Docker Hub is a public registry [https://hub.docker.com/](https://hub.docker.com/).

From Jake Growler, try `docker login` , because docker may not be using your ACR credentials provided by the tenant

And also make sure you’re logged into the ACR directly by running:

```zsh
az acr login --name hmctspublic --subscription DCD-CNP-DEV
```

I used `brew install az` to install Azure Command Line Interface (CLI) on my MacBook Pro.

And the finally step was the clincher for me. I logged out of the Azure Container Registry (ACR) and then logged back in with the correct credentials.

```zsh
docker logout hmctspublic.azurecr.io
```


Afterwards the invoke the command to start the application locally

```zsh
docker compose up
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

It finally works, I was able to surf to http://localhost:3206/health


Check that you in the correct Cloud Native Platform (CNP) Subscription with the Azure CLI.
At the terminal running `az account show` tells you that you are a part of DCD-CNP-DEV?



```zsh
az account show

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

## Double Check Azure Portal Access

Check that you can see the Azure Portal and HDP resources

https://portal.azure.com/#@HMCTS.NET/resource/subscriptions/8999dec3-0104-4a27-94ee-6588559729d1/resourceGroups/rpe-acr-prod-rg/providers/Microsoft.ContainerRegistry/registries/hmctspublic/overview

Here is a handy reference to Azure Enterprise docs

https://github.com/hmcts/azure-enterprise/blob/main/README.md



