FROM eclipse-temurin:21-jre as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:21-jre
WORKDIR application
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
# Keep this command,
# if nothing is present in snapshot-dependencies
# then docker will exit with error "layer does not exist"
RUN true
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/application/ ./

EXPOSE 8080

CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]