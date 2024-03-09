# Escolher uma imagem base do Java. Vamos usar uma com o Gradle incluído
# para facilitar o processo de build. Note que você pode optar por uma imagem apenas
# com o JRE se já tiver o JAR pronto para execução.
FROM gradle:jdk11 as builder

# Copiar o código fonte do projeto para a imagem
COPY --chown=gradle:gradle . /home/gradle/src

# Mudar para o diretório de trabalho onde o código fonte está
WORKDIR /home/gradle/src

# Rodar o build do projeto com Gradle. Isso vai gerar o JAR executável.
RUN gradle build --no-daemon

# Agora, vamos para a imagem de execução, que pode ser mais leve que a de build.
FROM openjdk:11-jre-slim

# Copiar o JAR do estágio de build para a imagem de execução.
# Isso presume que seu JAR está localizado no diretório 'build/libs' após o build.
# O nome do JAR deve corresponder ao nome gerado pelo seu Gradle build.
COPY --from=builder /home/gradle/src/build/libs/LSBTBot.jar /app/LSBTBot.jar

# O comando para executar o aplicativo.
CMD ["java", "-jar", "/app/LSBTBot.jar"]