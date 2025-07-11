FROM eclipse-temurin:17-jdk

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치 (Debian 계열이므로 apt 사용)
RUN apt-get update && apt-get install -y unzip curl tini bash

# tini를 ENTRYPOINT로 설정 (좀비 프로세스 방지)
ENTRYPOINT ["/usr/bin/tini", "--"]

# GitHub Actions에서 만든 app.jar 복사
COPY ./docker-output/app.jar project.jar

# promtail 다운로드 및 설정
RUN curl -L -o promtail.zip https://github.com/grafana/loki/releases/download/v2.9.2/promtail-linux-amd64.zip && \
    unzip promtail.zip && \
    mv promtail-linux-amd64 promtail && \
    chmod +x promtail && \
    rm promtail.zip

# promtail 설정 복사
COPY promtail-config.yaml /app/promtail-config.yaml

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# Spring Boot 애플리케이션 + Promtail 동시 실행
CMD bash -c "java -jar project.jar > /app/logs/app.log 2>&1 & ./promtail -config.file=/app/promtail-config.yaml & wait -n"