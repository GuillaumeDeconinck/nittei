# syntax=docker/dockerfile:1.7

ARG BUILDER_IMAGE=maven:3.9.11-eclipse-temurin-25
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-noble

FROM ${BUILDER_IMAGE} AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw .
COPY java java

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -f java/pom.xml -pl nittei-app -am package -DskipTests

FROM build AS extract

RUN mkdir -p /layers \
    && java -Djarmode=tools -jar java/nittei-app/target/nittei-app-0.1.0-SNAPSHOT.jar extract --launcher --destination /layers

FROM ${RUNTIME_IMAGE} AS runtime

ARG BUILD_DATE=unknown
ARG VCS_REF=unknown

LABEL org.opencontainers.image.title="nittei-java" \
      org.opencontainers.image.description="Nittei Java backend" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}"

ENV APP_HOME=/opt/nittei \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=""

WORKDIR ${APP_HOME}

RUN groupadd --system --gid 10001 nittei \
    && useradd --system --uid 10001 --gid 10001 --home-dir ${APP_HOME} --shell /usr/sbin/nologin nittei \
    && mkdir -p ${APP_HOME} \
    && chown -R nittei:nittei ${APP_HOME}

COPY --from=extract --chown=10001:10001 /layers/ ./

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
