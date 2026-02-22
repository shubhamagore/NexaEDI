FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy only backend folder
COPY backend ./backend

WORKDIR /app/backend

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["sh", "-c", "java -jar target/*.jar"]