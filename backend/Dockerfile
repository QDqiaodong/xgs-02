ARG DOCKER_REGISTRY=docker.m.daocloud.io

FROM ${DOCKER_REGISTRY}/library/maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY .m2 /root/.m2

COPY pom.xml .

RUN mvn -B dependency:resolve dependency:resolve-plugins

COPY src ./src

RUN mvn -B clean package -DskipTests

FROM ${DOCKER_REGISTRY}/library/eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/recipe-backend.jar app.jar

RUN apt-get update && \
    apt-get install -y --no-install-recommends tini && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /tmp/* /var/tmp/*

RUN mkdir -p /app/images

EXPOSE 8088

ENTRYPOINT ["tini", "--", "java", "-jar", "app.jar"]
