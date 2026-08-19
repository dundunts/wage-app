# ---------- Build stage ----------
FROM gradle:8.7-jdk21-jammy AS build
WORKDIR /workspace

# Копируем весь проект (т.к. модуль зависит от корня)
COPY . .

# Собираем runtime-артефакт; проверки выполняются CI до сборки образа.
RUN gradle :bootJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system --gid 10001 wage-app \
    && useradd --system --uid 10001 --gid wage-app --home-dir /app --shell /usr/sbin/nologin wage-app

# Копируем jar из выбранного модуля
COPY --from=build /workspace/build/libs/*.jar app.jar

RUN chown wage-app:wage-app app.jar

USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
