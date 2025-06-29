FROM eclipse-temurin:23-jdk-alpine AS builder
RUN apk add --no-cache maven
WORKDIR /workspace
COPY . .
RUN mvn -B -pl bot -am clean package -DskipTests

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/bot/target/bot-*.jar ./bot.jar
EXPOSE 8080 8090
ENTRYPOINT ["java","-jar","bot.jar"]
