FROM openjdk:8

ADD target/guestbook-0.0.1-SNAPSHOT.jar /app/guestbook.jar
ADD entrypoint.sh /app/
RUN chmod 755 /app/entrypoint.sh
ADD https://github.com/scouter-project/scouter/releases/download/v2.6.1/scouter-min-2.6.1.tar.gz /
RUN tar xfz scouter-min-2.6.1.tar.gz; \
    rm -fr scouter-min-2.6.1.tar.gz

ENV APP_HOME /app
EXPOSE 80
VOLUME /app/upload
WORKDIR $APP_HOME

ENTRYPOINT /app/entrypoint.sh
