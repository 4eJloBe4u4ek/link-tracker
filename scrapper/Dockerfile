FROM eclipse-temurin:23-jdk-alpine AS builder
RUN apk add --no-cache maven
WORKDIR /workspace
COPY . .
RUN mvn -B -pl scrapper -am clean package -DskipTests

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/scrapper/target/scrapper-*.jar ./scrapper.jar
EXPOSE 8081 8091
ENTRYPOINT ["java","-jar","scrapper.jar"]
