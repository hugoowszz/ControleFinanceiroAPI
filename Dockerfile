# Estágio de Build - usa Maven com JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY . .
RUN chmod +x ./mvnw

# Limita o uso de memória durante o build
ENV MAVEN_OPTS="-Xms128m -Xmx384m"
RUN ./mvnw clean package -DskipTests

# Estágio de Execução - usa JRE 21
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]