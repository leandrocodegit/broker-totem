# Utiliza uma imagem Maven com Java 17 (Eclipse Temurin)
FROM maven:3.8.6-eclipse-temurin-17 AS builder

# Define o diretório de trabalho no container
WORKDIR /app

# Copia o código fonte para o container
COPY . .

# Executa o Maven para compilar o projeto
RUN mvn clean package -DskipTests

# Usa uma imagem mais leve para executar o JAR compilado
FROM eclipse-temurin:17-jdk

# Define o diretório de trabalho para a aplicação
WORKDIR /app

# Copia o JAR do estágio de build para o estágio de runtime
COPY --from=builder /app/target/comando-1.0.0.jar /app/comando-1.0.0.jar

ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENV TZ=America/Sao_Paulo
ENV JAVA_OPTS="-Duser.timezone=America/Sao_Paulo"

# Porta em que a aplicação irá rodar
EXPOSE 8080 5011

# Comando para executar a aplicação
CMD ["java", "-jar", "/app/comando-1.0.0.jar"]
