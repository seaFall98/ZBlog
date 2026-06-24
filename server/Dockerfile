FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S zblog && adduser -S zblog -G zblog

COPY --from=builder /workspace/target/*.jar /app/zblog-server.jar

RUN mkdir -p /app/uploads && chown -R zblog:zblog /app

USER zblog

ENV SERVER_PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/zblog-server.jar"]
