# StockTest 프로젝트 작업 정리

## 개요

한국투자증권 Open API(KIS API)를 활용한 모의 주식 투자 웹 애플리케이션.

- **GitHub**: https://github.com/wldhel2tka/stockTest
- **커밋**: 2개 (`Initial commit` → `한국투자 API 주식 검색 및 캔들 차트 추가`)

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.3.4, Java 21, MyBatis, Gradle |
| Frontend | Vue.js 3 (Composition API), Vite, Bootstrap 5, Bootstrap Icons |
| DB | MySQL (`stockdb`) |
| 외부 API | 한국투자증권 Open API (KIS) |
| 차트 | lightweight-charts |

---

## 프로젝트 구조

```
stockTest/
├── backend/
│   └── src/main/java/com/stocktest/
│       ├── config/
│       │   ├── CorsConfig.java           # CORS 설정 (localhost:5173 허용)
│       │   └── RestTemplateConfig.java   # RestTemplate Bean 설정
│       ├── controller/
│       │   ├── AuthController.java       # 로그인/회원가입
│       │   ├── StockController.java      # 주식 검색, 현재가, 차트
│       │   ├── MockInvestmentController.java  # 모의투자 (매수/매도/포트폴리오)
│       │   └── RecommendedStockController.java # 추천 종목 (거래량/상승/하락)
│       ├── service/
│       │   ├── KisTokenService.java      # KIS API 토큰 발급 및 갱신
│       │   ├── StockService.java         # 주식 검색/현재가/차트 조회
│       │   ├── MockInvestmentService.java # 모의투자 비즈니스 로직
│       │   ├── RecommendedStockService.java # 거래량/등락률 순위 조회
│       │   └── UserService.java          # 회원 관리
│       ├── mapper/
│       │   ├── UserMapper.java
│       │   └── MockInvestmentMapper.java
│       ├── model/
│       │   ├── User.java
│       │   ├── VirtualAccount.java
│       │   ├── Portfolio.java / PortfolioItem.java / PortfolioResponse.java
│       │   ├── Transaction.java
│       │   ├── StockPrice.java
│       │   ├── StockSearchResult.java
│       │   ├── ChartCandle.java
│       │   └── RecommendedStock.java
│       └── resources/
│           ├── application.yml
│           ├── schema.sql
│           └── mapper/ (UserMapper.xml, MockInvestmentMapper.xml)
└── frontend/
    └── src/
        ├── views/
        │   ├── LoginView.vue       # 로그인 페이지
        │   ├── RegisterView.vue    # 회원가입 페이지
        │   ├── HomeView.vue        # 주식 검색 + 매수/매도 + 캔들 차트
        │   ├── PortfolioView.vue   # 포트폴리오 현황
        │   └── RecommendedView.vue # 추천 종목 (거래량/상승/하락)
        ├── api/index.js            # Axios API 호출 모음
        ├── router/index.js         # Vue Router 설정
        └── App.vue
```

---

## DB 테이블

### users
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| username | VARCHAR(50) UNIQUE | 아이디 |
| password | VARCHAR(255) | 비밀번호 |
| name | VARCHAR(50) | 이름 |
| company | VARCHAR(100) | 회사 |
| phone | VARCHAR(20) | 연락처 |
| email | VARCHAR(100) UNIQUE | 이메일 |
| user_type | ENUM('CUSTOMER','DEVELOPER') | 고객사/개발사 |
| created_at | DATETIME | 가입일 |

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
| stock_code | VARCHAR(10) | 종목코드 |
| stock_name | VARCHAR(50) | 종목명 |
| quantity | INT | 보유 수량 |
| avg_price | BIGINT | 평균 매수가 |

### transactions
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| user_id | BIGINT FK | |
| type | ENUM('BUY','SELL') | 매수/매도 |
| stock_code | VARCHAR(10) | 종목코드 |
| stock_name | VARCHAR(50) | 종목명 |
| quantity | INT | 거래 수량 |
| price | BIGINT | 체결 단가 |
| total_amount | BIGINT | 총 거래금액 |
| created_at | DATETIME | 거래일시 |

---

## API 엔드포인트

### 인증 (`/api/auth`)
| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/register` | 회원가입 |

### 주식 (`/api/stock`)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/stock/search?query=` | 종목 검색 (종목명/코드) |
| GET | `/api/stock/price?code=` | 현재가 조회 |
| GET | `/api/stock/chart?code=&period=&startDate=&endDate=` | 캔들 차트 (D/W/M/Y) |

### 모의투자 (`/api/mock`)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/mock/account?userId=` | 가상계좌 조회 (없으면 자동 생성) |
| GET | `/api/mock/portfolio?userId=` | 포트폴리오 조회 |
| POST | `/api/mock/buy` | 매수 |
| POST | `/api/mock/sell` | 매도 |
| GET | `/api/mock/transactions?userId=` | 거래내역 조회 |
| POST | `/api/mock/reset` | 계좌 초기화 (잔고 1천만원 리셋) |

### 추천 종목 (`/api/recommend`)
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/recommend/volume?market=J` | 거래량 상위 30개 |
| GET | `/api/recommend/gainers?market=J` | 상승률 상위 30개 |
| GET | `/api/recommend/losers?market=J` | 하락률 상위 30개 |

---

## 주요 기능 상세

### KIS API 연동
- **KisTokenService**: client_credentials 방식으로 Access Token 발급, 만료 10분 전 자동 갱신 (synchronized)
- **TR ID 매핑**:
  - `FHKST01010100` - 현재가 조회
  - `FHKST03010100` - 일/주/월/년 차트
  - `FHPST01710000` - 거래량 순위
  - `FHPST01700000` - 등락률 순위

### 주식 검색
- KOSPI 50개 + KOSDAQ 17개 종목 하드코딩 리스트
- 종목명 포함 검색 또는 6자리 코드 완전 일치 검색

### 모의투자 로직
- 매수: 현재가 × 수량만큼 잔고 차감, 평균단가 재계산
- 매도: 현재가 × 수량만큼 잔고 증가, 전량 매도 시 포트폴리오에서 삭제
- 계좌 없을 경우 자동 생성 (1천만원)

### 포트폴리오 계산
- 보유 종목별 현재가 실시간 조회
- 평가손익(PnL), 수익률(%) 계산
- 총 보유 자산 = 가용 잔고 + 보유 종목 평가금액

---

## 화면 구성

| 화면 | 경로 | 설명 |
|------|------|------|
| 로그인 | `/` | sessionStorage 기반 인증 |
| 회원가입 | `/register` | user_type 선택 포함 |
| 주식 검색 | `/home` | 검색 → 현재가 → 캔들차트 → 매수/매도 모달 |
| 포트폴리오 | `/portfolio` | 보유 종목, 손익, 거래내역, 계좌 초기화 |
| 추천 종목 | `/recommend` | 거래량/상승/하락 탭 전환 |

---

## 설정 정보

- **Backend 포트**: 8080
- **Frontend 포트**: 5173
- **DB**: MySQL `stockdb` (root / 1q2w3e4r)
- **KIS API Base URL**: `https://openapi.koreainvestment.com:9443`
- **schema.sql**: 앱 시작 시 `spring.sql.init.mode: always`로 자동 실행

---

## 실행 방법

```bash
# Backend
cd stockTest/backend
./gradlew bootRun

# Frontend
cd stockTest/frontend
npm install
npm run dev
```
