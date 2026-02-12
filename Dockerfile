# Используем Gradle с Java 21 для сборки
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

# Копируем файлы сборки
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Даем права на выполнение
RUN chmod +x gradlew

# Копируем исходный код
COPY src src

# Собираем приложение
RUN ./gradlew build --no-daemon -x test

# Финальный образ с Java 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем собранный JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Создаем пользователя для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]