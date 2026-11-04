# testcontainers

Published library `dynamic-form-testcontainers` so other projects can run the app in Docker tests.

- `DynamicFormContainer`: the backend image `ghcr.io/markoniemi/dynamic-form:<jar version>`, profile `prod`, waits on `/actuator/health`. Caller supplies Postgres (`withDatabase`) and auth settings (`withIssuerUri`, `withJwkSetUri`)
- `DynamicFormStack`: auth server + Postgres 16 + backend on one network, wired like `docker-compose.yaml`; default users `admin/admin`, `user/user` from `dynamicform/auth-defaults.yaml`; extra auth config via `withAuthConfigFile`
- The image tag comes from `dynamicform/version.properties`, filtered with the Maven project version at build time

## Commands (from repo root)
- ITs: `mvn -pl testcontainers verify` (failsafe, `DynamicFormStackIT`; Docker must be running)

## Pitfalls
- The IT needs the image for the current version to exist locally: build `backend` first (`mvn -pl backend verify` or `mvn verify` from root runs jib `dockerBuild`)
- The auth server is bound to host port 9000 and issues tokens for `http://localhost:9000`; the `iss` claim must equal the backend's issuer. Nothing else may hold port 9000 (backend ITs release it on JVM exit)
- When modifying the auth container's host config, modify the existing `HostConfig` rather than replacing it, or the network mode is lost
