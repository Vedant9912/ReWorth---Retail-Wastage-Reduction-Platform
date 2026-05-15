FROM eclipse-temurin:17

WORKDIR /app

COPY . .

RUN mvn clean package

CMD ["sh", "-c", "java -jar target/*.jar"]
