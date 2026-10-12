# Backend image, built from source in one multi-stage build.
# Build context: backend/
#   `docker build -f backend/Dockerfile --build-arg GIT_SHA=<commit> -t northstar-crm-backend:local backend`
# Runs as a non-root user
# user id in group 0: everything the app reads is group-readable, and the only writable place is /tmp.
# TODO(team): pin both base images by digest once the registry is reachable (Dependabot's docker ecosystem then keeps them current).
ARG BUILD_IMAGE=eclipse-temurin:21-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${BUILD_IMAGE} AS build
WORKDIR /src
# Dependencies first, so this layer is cached until the pom changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN bash ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN bash ./mvnw -B -ntp -DskipTests package \
 && bash -c 'set -e; jars=(target/*.jar); [ "${#jars[@]}" -eq 1 ] || { echo "expected exactly one jar" >&2; exit 1; }; cp "${jars[0]}" /application.jar'

FROM ${RUNTIME_IMAGE} AS extract
WORKDIR /extract
COPY --from=build /application.jar .
# Splits the Spring Boot jar into layers (dependencies change rarely, the application often).
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM ${RUNTIME_IMAGE}
ARG GIT_SHA=unknown
LABEL org.opencontainers.image.title="northstar-crm-backend" \
      org.opencontainers.image.revision="${GIT_SHA}"
WORKDIR /app
COPY --from=extract --chown=10001:0 --chmod=g=u /extract/extracted/dependencies/ ./
COPY --from=extract --chown=10001:0 --chmod=g=u /extract/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=10001:0 --chmod=g=u /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=10001:0 --chmod=g=u /extract/extracted/application/ ./
USER 10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# No HEALTHCHECK: OpenShift ignores it; the Deployment's probes are the health contract (openshift/crm-api.yaml).
ENTRYPOINT ["java", "-jar", "application.jar"]
