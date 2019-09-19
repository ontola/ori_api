FROM gradle:latest AS builder

COPY . /usr/src/ori_api
WORKDIR /usr/src/ori_api

RUN gradle --no-scan clean dep build
RUN tar -xf /usr/src/ori_api/build/distributions/ori_api.tar -C /usr/src/ori_api/build/distributions/

FROM adoptopenjdk:8-openj9

RUN mkdir -p /var/opt/ori_data/id
COPY --from=builder /usr/src/ori_api/build/distributions/ori_api /usr/src/ori_api

ENV APP_HOME "/usr/src/ori_api"
WORKDIR /usr/src/ori_api

ENV BUGSNAG_KEY ""
ENV DELTA_TOPIC "ori-delta"
ENV KAFKA_ADDRESS ""
ENV KAFKA_HOSTNAME "localhost"
ENV KAFKA_PORT "9092"
ENV KAFKA_GROUP_ID "ori_api"
ENV BASE_IRI "https://id.openraadsinformatie.nl"
ENV SUPPLANT_IRI "http://purl.org/link-lib/supplant"
ENV THREAD_COUNT 1
ENV KAFKA_USERNAME ""
ENV KAFKA_PASSWORD ""
ENV DATA_DIR "/var/opt/ori_data/id"
ENV REDIS_HOSTNAME="redis"
ENV REDIS_PORT="6379"

VOLUME /var/opt/ori_data/id

CMD "bin/ori_api"
