FROM clojure:temurin-21-tools-deps-alpine AS builder

WORKDIR /build

# Copy dependency files
COPY deps.edn /build/
# Download dependencies
RUN clojure -P

# Copy source and build
COPY . /build/
RUN clojure -T:build uber

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built uberjar
COPY --from=builder /build/target/*-standalone.jar /app/app.jar

CMD ["sh", "-c", "if [ -n \"$CONFIG_FILE\" ]; then exec java -Dconfig=\"$CONFIG_FILE\" -jar /app/app.jar; else exec java -jar /app/app.jar; fi"]
