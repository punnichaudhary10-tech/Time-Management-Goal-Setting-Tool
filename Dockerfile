FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY lib/mysql-connector-j-26.7.0.jar /app/lib/mysql-connector.jar
COPY src /app/src
COPY frontend /app/frontend

RUN mkdir -p /app/classes && \
    javac -cp "/app/lib/mysql-connector.jar" \
    -d /app/classes \
    $(find /app/src -name "*.java")

EXPOSE 8080

CMD ["java", "-cp", "/app/classes:/app/lib/mysql-connector.jar", "server.WebServer"]
