# Stock News Bot

구독한 종목의 뉴스를 AI로 요약해 텔레그램으로 전송하고, DART 공시·시세·급변동 알림을 제공하는 Spring Boot 기반 텔레그램 봇

---

## 주요 기능

| 기능 | 상태 |
|---|---|
| 텔레그램 봇 명령어 처리 (`/add`, `/remove`, `/list`) | 완료 |
| 네이버 검색 API 기반 종목 뉴스 수집 및 발송 | 완료 |
| Claude AI 뉴스 요약 및 영향도 분석 | 개발 중 |
| 한국투자증권 KIS API 현재가 조회 | 개발 중 |
| 급변동 알림 (기준가 대비 ±N%) | 개발 중 |
| 금융감독원 DART 공시 알림 | 개발 중 |
| Docker 컨테이너 배포 | 개발 중 |

---

## 기술 스택

- **Language**: Java 21 (Virtual Thread)
- **Framework**: Spring Boot 4.1.0 (Spring Framework 7, Jakarta EE 11)
- **Build**: Gradle 8.14+ (Groovy DSL)
- **DB**: SQLite (WAL 모드, Hibernate Community Dialects)
- **HTTP**: Spring RestClient
- **Infra**: Docker (멀티스테이지 빌드), Docker Compose

### 외부 API

| API | 용도 |
|---|---|
| Telegram Bot API | long-polling 기반 봇 통신 |
| 네이버 검색 API | 종목 뉴스 수집 |
| Anthropic Messages API | 뉴스 요약 및 영향도 분석 |
| 한국투자증권 KIS API | 현재가 시세 조회 |
| 금융감독원 OpenDART API | 공시 수집 |

---

## 프로젝트 구조

```
com.example.stockbot
├── config/
│   ├── AppProperties.java          # @ConfigurationProperties 설정 바인딩
│   └── RestClientConfig.java       # RestClient 빈 설정
├── common/
│   └── TextUtil.java               # SHA-256 해시, HTML 처리 유틸
├── domain/
│   ├── Subscription.java           # 구독 엔티티
│   ├── SubscriptionRepository.java
│   ├── SentNews.java               # 발송 이력 엔티티 (중복 방지)
│   └── SentNewsRepository.java
├── subscription/
│   └── SubscriptionService.java    # 구독 CRUD
├── telegram/
│   ├── TelegramClient.java         # sendMessage, getUpdates
│   └── TelegramBotService.java     # long-polling 루프 + 명령어 처리
├── news/
│   ├── NaverNewsClient.java        # 네이버 뉴스 검색
│   └── NewsService.java            # 폴링 → 중복 필터 → 발송
├── ai/
│   └── ClaudeClient.java           # Anthropic Messages API (예정)
├── dart/
│   ├── DartClient.java             # DART API (예정)
│   ├── CorpCodeService.java        # 종목코드 매핑 (예정)
│   └── DartService.java            # 공시 폴링 (예정)
├── price/
│   ├── KisTokenManager.java        # KIS OAuth 토큰 관리 (예정)
│   ├── KisClient.java              # 현재가 조회 (예정)
│   ├── PriceService.java           # 정기 시세 발송 (예정)
│   └── VolatilityService.java      # 급변동 알림 (예정)
└── scheduler/
    └── ScheduledTasks.java         # @Scheduled 스케줄러 모음
```

---

## 시작하기

### 사전 요구사항

- Java 21
- 텔레그램 봇 토큰 (`@BotFather`에서 발급)
- 네이버 검색 API 키 ([developers.naver.com](https://developers.naver.com))

### 환경변수 설정

`.env.example`을 복사해서 `.env`를 만들고 실제 값을 입력

```bash
cp .env.example .env
```

```env
TELEGRAM_TOKEN=your_telegram_bot_token_here

NAVER_CLIENT_ID=your_naver_client_id_here
NAVER_CLIENT_SECRET=your_naver_client_secret_here
```

### 실행

```bash
./gradlew bootRun
```

### 헬스체크

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## 텔레그램 봇 명령어

| 명령어 | 설명 | 예시 |
|---|---|---|
| `/add [종목코드] [종목명]` | 종목 구독 | `/add 041510 에스엠` |
| `/remove [종목코드]` | 구독 취소 | `/remove 041510` |
| `/list` | 구독 종목 목록 조회 | `/list` |
| `/price [종목코드]` | 현재가 조회 (개발 중) | `/price 041510` |
| `/start` | 도움말 | `/start` |

---

## 아키텍처 특징

**Long-polling 방식 채택**
- Webhook은 공인 IP/도메인이 필요하지만, Long-polling은 로컬 환경에서도 동작하며 로컬 개발 편의를 위해 Long-polling 채택  
- Java 21 Virtual Thread를 활용해 블로킹 I/O 비용을 최소화

**중복 발송 방지**
- 뉴스 URL을 SHA-256으로 해시해서 `sent_news` 테이블에 저장 
- 재시작 후에도 이미 발송한 뉴스는 다시 보내지 않음

**외부 라이브러리 최소화**
- Spring Boot가 제공하는 `RestClient`, `@Scheduled`, `Spring Data JPA`만으로 외부 API 연동과 스케줄링을 구현

---

## 개발 로드맵

- [x] 프로젝트 부트스트랩
- [x] 텔레그램 봇 기본
- [x] 구독 관리 + SQLite
- [x] 뉴스 수집 (네이버 검색 API)
- [ ] Claude AI 요약
- [ ] 시세 발송 (KIS)
- [ ] 급변동 알림
- [ ] DART 공시
- [ ] Docker 완성 + 운영

---

## 동작 화면

### 뉴스 알림
![뉴스 알림](docs/images/news-01.png)

### 명령어
![명령어 동작](docs/images/chat-01.png)

---

## 라이선스

MIT