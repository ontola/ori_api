FROM maven:latest AS builder

COPY . /usr/src/ori_api
WORKDIR /usr/src/ori_api

RUN mvn clean package

FROM openjdk:13-alpine

RUN mkdir -p /var/opt/ori_data/id

COPY --from=builder /usr/src/ori_api/target /usr/src/ori_api
WORKDIR /usr/src/ori_api

ENV DELTA_TOPIC "ori-delta"
ENV KAFKA_ADDRESS ""
ENV KAFKA_HOSTNAME "localhost"
ENV KAFKA_PORT "9092"
ENV KAFKA_GROUP_ID "ori_api"
ENV BASE_IRI "https://id.openraadsinformatie.nl"
ENV SUPPLANT_IRI "http://purl.org/link-lib/supplant"

VOLUME /var/lib/data/id

CMD ["sh", \
    "-c", \
    "java \
    -Dio.ontola.ori_api.kafka.topic=${DELTA_TOPIC} \
    -Dio.ontola.ori_api.kafka.address=${KAFKA_ADDRESS} \
    -Dio.ontola.ori_api.kafka.hostname=${KAFKA_HOSTNAME} \
    -Dio.ontola.ori_api.kafka.port=${KAFKA_PORT} \
    -Dio.ontola.ori_api.kafka.group_id=${KAFKA_GROUP_ID} \
    -Dio.ontola.ori_api.supplant_iri=${SUPPLANT_IRI} \
    -Dio.ontola.ori_api.root_data_dir=/var/opt/ori_data/id \
    -jar ori_api-1.0-SNAPSHOT.jar"]
