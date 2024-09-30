FROM bladex/alpine-java:openjdk8-openj9_cn_slim

MAINTAINER smallchill@163.com

RUN mkdir -p /zhaocai-auth

WORKDIR /zhaocai-auth

EXPOSE 8800

ADD ./target/zhaocai-auth.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

CMD ["--spring.profiles.active=test"]