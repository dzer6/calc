FROM amazoncorretto:22-alpine

WORKDIR /usr/src/app

ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/v1.32.0/aws-opentelemetry-agent.jar /usr/src/app/opentelemetry-javaagent.jar
RUN chmod 644 /usr/src/app/opentelemetry-javaagent.jar

COPY ./target/app.jar /usr/src/app/

ENV JAVA_TOOL_OPTIONS="-javaagent:/usr/src/app/opentelemetry-javaagent.jar \
 -XX:FlightRecorderOptions=stackdepth=256 \
 -XX:+UseContainerSupport \
 -XX:MaxRAMPercentage=90.0"

EXPOSE 8080

CMD ["java", "-server", "-jar", "/usr/src/app/app.jar"]