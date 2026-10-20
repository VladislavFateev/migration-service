FROM vsm-registry.itfbgroup.ru/eclipse-temurin:21-jre-alpine

ENV LANG ru_RU.UTF-8
ENV JAVA_OPTS=""

RUN chmod 777 /mnt
RUN addgroup -S jboss && adduser -S jboss -G jboss -h /app

USER jboss

WORKDIR /app

ADD target/migration-service.jar app.jar

EXPOSE 8082

ENTRYPOINT java $JAVA_OPTS -Djava.security.egd=file:/dev/urandom -jar app.jar
