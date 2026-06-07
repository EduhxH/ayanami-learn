FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts gradle.properties settings.backend.gradle.kts ./
COPY src src

RUN sed -i '/^org.gradle.java.home=/d' gradle.properties \
    && chmod +x gradlew \
    && ./gradlew -c settings.backend.gradle.kts clean installShadowDist --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV DEBUG_SAVE_VOICE_UPLOADS=false
ENV PORT=8080

COPY --from=build /workspace/build/install/AyanamiLearnBackend-shadow/ ./

EXPOSE 8080

CMD ["./bin/AyanamiLearnBackend"]
