FROM maven:latest AS builder

COPY . /usr/src/ori_api
WORKDIR /usr/src/ori_api

RUN mvn clean package

FROM openjdk:13-alpine

RUN mkdir -p /var/opt/ori_data/id

COPY --from=builder /usr/src/ori_api/target/ori.api-1.0-SNAPSHOT.jar /usr/src/ori_api/ori.api-1.0-SNAPSHOT.jar
WORKDIR /usr/src/ori_api

ENV DELTA_TOPIC "ori-delta"
ENV KAFKA_ADDRESS ""
ENV KAFKA_HOSTNAME "localhost"
ENV KAFKA_PORT "9092"
ENV KAFKA_GROUP_ID "ori_api"
ENV BASE_IRI "https://id.openraadsinformatie.nl"
ENV SUPPLANT_IRI "http://purl.org/link-lib/supplant"
ENV THREAD_COUNT 1
ENV CLUSTER_API_KEY ""
ENV CLUSTER_API_SECRET ""

VOLUME /var/lib/data/id

CMD ["sh", \
    "-c", \
    "java \
    -Dio.ontola.ori.api.kafka.topic=${DELTA_TOPIC} \
    -Dio.ontola.ori.api.kafka.address=${KAFKA_ADDRESS} \
    -Dio.ontola.ori.api.kafka.hostname=${KAFKA_HOSTNAME} \
    -Dio.ontola.ori.api.kafka.port=${KAFKA_PORT} \
    -Dio.ontola.ori.api.kafka.group_id=${KAFKA_GROUP_ID} \
    -Dio.ontola.ori.api.supplantIRI=${SUPPLANT_IRI} \
    -Dio.ontola.ori.api.dataDir=/var/opt/ori_data/id \
    -Dio.ontola.ori.api.threads=${THREAD_COUNT} \
    -Dio.ontola.ori.api.kafka.clusterApiKey=${CLUSTER_API_KEY} \
    -Dio.ontola.ori.api.kafka.clusterApiSecret=${CLUSTER_API_SECRET} \
    -jar ori.api-1.0-SNAPSHOT.jar"]
