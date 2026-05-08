# StockTest 프로젝트 — Claude 작업 컨텍스트

## 프로젝트 목표

한국투자증권 KIS API를 활용한 **주식 자동매매 Android 앱** 개발.
차트 데이터를 ML 모델로 학습하여 BUY/SELL/HOLD 신호를 생성하고, 실계좌에 자동 주문한다.

---

## 확정된 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| 모바일 | Android 네이티브 (Kotlin) | |
| 백엔드 | Spring Boot 3.3.4 (기존 재활용) | JWT 인증 추가 필요 |
| ML 서비스 | Python FastAPI | 별도 서버, Spring Boot에서 HTTP 호출 |
| ML 모델 | Random Forest / LSTM | 일봉 OHLCV + 기술적 지표 피처 |
| DB | MySQL `stockdb` | root / 1q2w3e4r |
| 외부 API | KIS API (실전투자) | |

---

## 전체 아키텍처

```
Android 앱 (Kotlin)
      ↕  REST API + JWT
Spring Boot (포트 8080)
      ↕  KIS API (주가 조회 / 실계좌 주문)
      ↕  HTTP 호출
Python ML 서비스 (포트 8000)
      ↕  학습 데이터 (KIS 과거 차트)
```

---

## 프로젝트 구조 (목표)

```
stockTest/
├── backend/        — Spring Boot (기존, JWT 추가 + ML 클라이언트 추가)
├── ml/             — Python FastAPI ML 서비스 (신규)
│   ├── train.py
│   ├── predict.py
│   ├── model/
│   └── requirements.txt
├── android/        — Android Kotlin 앱 (신규)
└── CLAUDE.md
```

`frontend/` (Vue.js)는 사용하지 않음. Android 앱으로 대체.

---

## KIS API 정보

```yaml
base-url: https://openapi.koreainvestment.com:9443
ws-url:   ws://ops.koreainvestment.com:21000
app-key:  PShM1BBjZaTnwrRQhYom6xwufHsBrfkXA6Uv
account-no: 64120083-01
```

> `app-secret`은 `backend/src/main/resources/application.yml` 참고

---

## GitHub 정보

- 레포: `https://github.com/wldhel2tka/stockTest`
- 브랜치: `test`

---

## MCP 설정 (Claude Code)

Claude Code에 다음 MCP가 설치되어 있음 (`C:\Users\INDG-10\.claude.json`):

| MCP | 용도 |
|-----|------|
| `github` | GitHub 레포 관리 (PAT 인증) |
| `mysql` | `stockdb` 직접 쿼리 |
| `playwright` | 브라우저 자동화 / 스크래핑 |
| `atlassian` | Jira / Confluence |
| `claude.ai Google Drive` | 파일 저장 |

MySQL MCP 환경변수: `MYSQL_HOST=localhost`, `MYSQL_USER=root`, `MYSQL_PASS=1q2w3e4r`, `MYSQL_DB=stockdb`

---

## 개발 순서

### 1단계 — Python ML 서비스 (`stockTest/ml/`)
- [ ] KIS API로 과거 일봉 데이터 수집 스크립트 작성
- [ ] 피처 생성: RSI(14), MACD, 볼린저밴드, 5MA, 20MA + OHLCV
- [ ] 레이블링: N일 후 수익률 기준 BUY/SELL/HOLD
- [ ] Random Forest 모델 학습 및 저장
- [ ] FastAPI `/predict` 엔드포인트 구현

### 2단계 — Spring Boot 수정 (`stockTest/backend/`)
- [ ] JWT 인증 추가 (Spring Security 또는 jjwt)
- [ ] Python ML 서비스 호출 클라이언트 추가
- [ ] `AutoTradingService` ML 예측 기반으로 교체
- [ ] FCM 푸시 알림 연동

### 3단계 — Android 앱 (`stockTest/android/`)
- [ ] 프로젝트 생성 (Kotlin, API 26+)
- [ ] 로그인 / 회원가입 화면
- [ ] 종목 검색 + 캔들차트 (MPAndroidChart)
- [ ] ML 분석 결과 표시
- [ ] 자동매매 ON/OFF + 설정 화면
- [ ] 포트폴리오 + 거래내역

---

## 기존 백엔드에서 유지할 코드

| 클래스 | 처리 |
|--------|------|
| `KisTokenService` | 유지 — KIS 토큰 발급/갱신 |
| `StockService` | 유지 — 현재가, 차트 조회 |
| `RealOrderService` | 유지 — 실계좌 매수/매도 |
| `MockInvestmentService` | 유지 |
| `UserService` | 유지 (JWT 방식으로 수정) |
| `TechnicalAnalysisService` | ML 예측으로 교체 예정 |
| `AutoTradingService` | ML 예측 기반으로 교체 예정 |
| `KisWebSocketService` | 유지 — 실시간 체결 |

---

## 실행 방법

```bash
# Backend
cd stockTest/backend
./gradlew bootRun

# ML 서비스 (개발 예정)
cd stockTest/ml
uvicorn main:app --reload --port 8000

# Android
# Android Studio에서 stockTest/android 열기
```

---

## 주의사항

- 실계좌 자동매매는 실제 돈이 거래됨. 초기에는 소액 한도 설정 필수
- `application.yml`에 API 키가 평문으로 저장되어 있음 — 운영 배포 시 환경변수 분리 필요
- 자동매매 장 시간: 평일 09:00~15:30 KST
