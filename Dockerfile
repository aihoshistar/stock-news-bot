# Dockerfile

# ── 1단계: 빌드 ──────────────────────────────────────
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# 의존성 캐시 레이어 (소스 변경 시 재다운로드 방지)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon --quiet || true

# 소스 복사 및 빌드
COPY src ./src
RUN gradle bootJar --no-daemon --quiet

# ── 2단계: 런타임 ─────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

# SQLite 데이터 디렉터리 생성
RUN mkdir -p /app/data

# JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 사용자 실행
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]