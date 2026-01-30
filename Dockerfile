# ---------- Build stage ----------
FROM gradle:8.7-jdk21-jammy AS build
WORKDIR /workspace

# Копируем весь проект (т.к. модуль зависит от корня)
COPY . .

# Собираем конкретный модуль
RUN gradle :build -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Копируем jar из выбранного модуля
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]