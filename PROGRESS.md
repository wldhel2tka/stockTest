# StockTest 프로젝트 작업 정리

## 개요

한국투자증권 Open API(KIS API)를 활용한 주식 투자 웹 애플리케이션.
모의투자 + 실계좌 주문 + 기술적 분석 + 자동매매 기능을 포함한다.

- **GitHub**: https://github.com/wldhel2tka/stockTest
- **Branch**: `test`

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.3.4, Java 21, MyBatis, Gradle |
| Frontend | Vue.js 3 (Composition API), Vite, Bootstrap 5, Bootstrap Icons |
| DB | MySQL (`stockdb`) |
| 외부 API | 한국투자증권 Open API (KIS), YouTube Data API v3 |
| 차트 | lightweight-charts |

---

## 실행 방법

```bash
# Backend (포트 8080)
cd stockTest/backend
./gradlew bootRun

# Frontend (포트 5173)
cd stockTest/frontend
npm install
npm run dev
```

- **DB**: MySQL `stockdb` (root / 1q2w3e4r)
- **KIS API Base URL**: `https://openapi.koreainvestment.com:9443` (실전투자)
- schema.sql이 앱 시작 시 자동 실행됨 (`spring.sql.init.mode: always`)

---

## 프로젝트 구조

```
stockTest/
├── backend/src/main/java/com/stocktest/
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── RestTemplateConfig.java
│   ├── controller/
│   │   ├── AuthController.java               # 로그인/회원가입
│   │   ├── StockController.java              # 주식 검색·현재가·차트
│   │   ├── MockInvestmentController.java     # 모의투자 매수/매도
│   │   ├── RecommendedStockController.java   # 추천 종목
│   │   ├── RealtimeController.java           # 실시간 체결 (SSE)
│   │   ├── TechnicalAnalysisController.java  # 기술적 분석 신호
│   │   ├── RealOrderController.java          # 실계좌 주문
│   │   ├── AutoTradingController.java        # 자동매매 설정/로그
│   │   ├── NewsController.java
│   │   └── YoutubeController.java
│   ├── service/
│   │   ├── KisTokenService.java              # KIS 토큰 발급·갱신
│   │   ├── StockService.java                 # 주식 정보 조회
│   │   ├── MockInvestmentService.java        # 모의투자 로직
│   │   ├── TechnicalAnalysisService.java     # RSI·MA·거래량 분석
│   │   ├── RealOrderService.java             # 실계좌 주문·잔고·체결내역
│   │   ├── AutoTradingService.java           # 자동매매 스케줄러
│   │   ├── RecommendedStockService.java
│   │   ├── KisWebSocketService.java
│   │   └── UserService.java
│   ├── model/
│   │   ├── StockSignal.java                  # 기술적 분석 결과
│   │   ├── AutoTradeLog.java                 # 자동매매 로그
│   │   └── (기존 모델들)
│   └── resources/
│       ├── application.yml
│       ├── schema.sql
│       └── mapper/
└── frontend/src/
    ├── views/
    │   ├── HomeView.vue        # 주식 검색·차트·매수/매도·기술적 분석 신호
    │   ├── PortfolioView.vue   # 포트폴리오·자동매매 패널
    │   ├── RealTradingView.vue # 실계좌 주문 페이지
    │   ├── YoutubeView.vue
    │   ├── NewsView.vue
    │   └── IpoView.vue
    ├── components/
    │   └── AppLayout.vue       # 사이드바 레이아웃
    ├── api/index.js
    └── router/index.js
```

---

## DB 테이블

### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE | 아이디 |
| password | VARCHAR(255) | |
| name | VARCHAR(50) | |
| email | VARCHAR(100) UNIQUE | |
| user_type | ENUM('CUSTOMER','DEVELOPER') | |
| created_at | DATETIME | |

### virtual_account
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| balance | BIGINT | 잔고 (기본 10,000,000원) |

### portfolio
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| stock_code | VARCHAR(10) | |
| stock_name | VARCHAR(50) | |
| quantity | INT | |
| avg_price | BIGINT | 평균 매수가 |

### transactions
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| type | ENUM('BUY','SELL') | |
| stock_code | VARCHAR(10) | |
| stock_name | VARCHAR(50) | |
| quantity | INT | |
| price | BIGINT | 체결 단가 |
| total_amount | BIGINT | |
| created_at | DATETIME | |

---

## API 엔드포인트

### 인증 `/api/auth`
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/register` | 회원가입 |

### 주식 `/api/stock`
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/stock/search?query=` | 종목 검색 |
| GET | `/api/stock/price?code=` | 현재가 조회 |
| GET | `/api/stock/chart?code=&period=&startDate=&endDate=` | 일/주/월 차트 |
| GET | `/api/stock/chart/minute?code=&type=` | 분봉 차트 |

### 모의투자 `/api/mock`
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/mock/account?userId=` | 가상계좌 조회 |
| GET | `/api/mock/portfolio?userId=` | 포트폴리오 조회 |
| POST | `/api/mock/buy` | 매수 `{userId, code, quantity}` |
| POST | `/api/mock/sell` | 매도 `{userId, code, quantity}` |
| GET | `/api/mock/transactions?userId=` | 거래내역 |
| POST | `/api/mock/reset` | 계좌 초기화 (잔고 1천만원) |

### 기술적 분석 `/api/analysis`
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/analysis/signal?code=` | RSI·이동평균·거래량 분석 |

응답 예시:
```json
{
  "code": "005930",
  "rsi": 77.2,
  "rsiSignal": "SELL",
  "ma5": 218300,
  "ma20": 200750,
  "maSignal": "BUY",
  "volumeRatio": 1.23,
  "volumeSignal": "NORMAL",
  "signal": "NEUTRAL",
  "score": 0,
  "description": "RSI 77.2 (과매수) · 5MA > 20MA (상승세)"
}
```

### 실계좌 주문 `/api/real`
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/real/order` | 매수/매도 주문 |
| GET | `/api/real/balance` | 실계좌 잔고 조회 |
| GET | `/api/real/orders` | 일별 주문체결 내역 |

주문 요청 예시:
```json
{ "code": "005930", "quantity": 1, "side": "BUY", "orderType": "MARKET" }
{ "code": "005930", "quantity": 1, "side": "SELL", "orderType": "LIMIT", "price": 75000 }
```

### 자동매매 `/api/auto-trading`
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/auto-trading/status` | 현재 설정·상태 조회 |
| POST | `/api/auto-trading/toggle` | ON/OFF `{enabled: true/false}` |
| POST | `/api/auto-trading/config` | 설정 변경 `{takeProfitPct, stopLossPct, buyAmount}` |
| GET | `/api/auto-trading/logs` | 자동매매 로그 조회 |
| POST | `/api/auto-trading/run-now` | 즉시 실행 |

---

## 주요 기능 상세

### KIS API 인증
- `KisTokenService`: client_credentials 방식으로 Access Token 발급, 만료 10분 전 자동 갱신
- TR ID 매핑:

| TR ID | 기능 |
|-------|------|
| `FHKST01010100` | 현재가 조회 |
| `FHKST03010100` | 일/주/월 차트 |
| `FHKST03010200` | 분봉 차트 |
| `TTTC0802U` | 실전 매수 주문 |
| `TTTC0801U` | 실전 매도 주문 |
| `TTTC8434R` | 실계좌 잔고 조회 |
| `TTTC8001R` | 일별 주문체결 조회 |

### 기술적 분석 (TechnicalAnalysisService)
최근 90일 일봉 데이터를 기반으로 3가지 지표 계산:

| 지표 | 계산 방식 | 매수 신호 | 매도 신호 |
|------|----------|----------|----------|
| RSI(14) | 14일 평균 상승폭 / 평균 하락폭 | ≤ 30 (과매도) | ≥ 70 (과매수) |
| 이동평균 | 5일선 vs 20일선 | 5MA > 20MA | 5MA < 20MA |
| 거래량 | 오늘 / 20일 평균 | 1.5배 이상이면 급등 표시 | - |

종합 점수(score) 합산 → ≥+2: BUY / ≤-2: SELL / 그 외: NEUTRAL

### 자동매매 (AutoTradingService)
- `@Scheduled(fixedDelay = 60_000)` — 1분 간격 실행
- 장 시간(09:00~15:30, 평일)에만 동작
- **자동 매도**: 보유 종목 수익률이 익절 기준 이상 또는 손절 기준 이하 도달 시 전량 매도
- **자동 매수**: WATCH_LIST(20개 종목) 중 BUY 신호 + 미보유 종목 발견 시 설정 금액으로 매수 (1회 최대 2종목)
- 기본 설정: 익절 +10%, 손절 -5%, 1회 매수금액 100만원

### 모의투자 로직
- 매수: 현재가 × 수량 잔고 차감, 기존 보유 시 평균단가 재계산
- 매도: 현재가 × 수량 잔고 증가, 전량 매도 시 포트폴리오에서 삭제
- 계좌 없으면 자동 생성 (1천만원)

---

## 화면 구성

| 화면 | 경로 | 설명 |
|------|------|------|
| 로그인 | `/` | sessionStorage 기반 인증 |
| 회원가입 | `/register` | |
| 주식 검색 | `/home` | 검색 → 현재가 → 기술적 분석 신호 → 캔들차트 → 매수/매도 |
| 포트폴리오 | `/portfolio` | 자동매매 패널 + 보유종목 + 거래내역 |
| 실계좌 주문 | `/real` | 실계좌 잔고·보유종목·주문폼·체결내역 |
| 유튜버 분석 | `/youtube` | 유튜버 영상 기반 종목 분석 |
| 뉴스 분석 | `/news` | 뉴스 기반 종목 분석 |
| 공모주 일정 | `/ipo` | IPO 청약 일정 |

---

## KIS API 계좌 정보 (application.yml)

```yaml
kis:
  app-key: PShM1BBjZaTnwrRQhYom6xwufHsBrfkXA6Uv
  base-url: https://openapi.koreainvestment.com:9443
  account-no: 64120083-01
```

> `app-secret`은 application.yml에 직접 저장됨. 운영 배포 시 환경변수로 분리 필요.
