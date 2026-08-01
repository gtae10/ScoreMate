# ScoreMate ⚾

KBO(한국프로야구) 일정·순위·선수 정보를 자동으로 수집해서 보여주는 스포츠 정보 앱입니다.
FotMob 같은 해외 스포츠 앱을 참고해서, "경기 일정/결과 → 순위표 → 선수 검색"을 한 곳에서
확인할 수 있게 만들었습니다.

> 개인 학습/포트폴리오 목적의 비상업적 프로젝트입니다. KBO 공식 사이트의 데이터를 크롤링해서
> 사용하며, 수집한 데이터의 저작권은 KBO(한국야구위원회)에 있습니다. 팀 엠블럼 등 저작권이
> 있는 이미지는 사용하지 않고, 팀 컬러 + 영문 약자 배지로 대체했습니다.

## 주요 기능

- **경기 일정/결과** — 날짜별 KBO 경기 조회, 진행중인 경기는 실시간(30초 주기)으로 갱신
- **순위표** — 시즌 팀 순위(승/패/무, 승률, 게임차)
- **선수 검색** — 이름으로 선수 검색, 상세 페이지에서 타자/투수 시즌 기록 확인
  - 타자: 타율/안타/홈런/타점/출루율/장타율/OPS 등
  - 투수: 방어율/승/패/세이브 등
- **팀별 전체 로스터** — 리더보드 상위권이 아닌 선수들까지 전체 등록 명단 확보

## 기술 스택

- **Backend**: Java 17, Spring Boot 4.1.0, Spring MVC, Spring Data JPA, Spring Security
- **View**: Thymeleaf (서버 사이드 렌더링)
- **DB**: MySQL
- **크롤링**: Jsoup (HTML 파싱, ASP.NET UpdatePanel 비동기 포스트백 직접 구현)
- **테스트**: JUnit 5, AssertJ, Spring Boot Test
- **빌드**: Gradle

## 아키텍처 개요

```
domain/
  match/     경기 일정·결과 (Match, League)
  team/      팀, 순위표 (Team, Standing)
  player/    선수, 시즌 기록 (Player, PlayerRecord)
  user/      회원
  prediction/  승부 예측 (부가 기능, 후순위)
  meetup/    직관 모임 매칭 (부가 기능, 후순위)

crawler/
  KboCrawler          경기 일정/결과 — AJAX(POST) + JSON 응답 파싱
  ScoreBoardCrawler    실시간 스코어 — 서버 렌더링 페이지 GET 파싱
  StandingCrawler      순위표 — 서버 렌더링 페이지 GET 파싱
  PlayerCrawler        타자/투수 리더보드 — 서버 렌더링 페이지 GET 파싱
  PlayerRosterCrawler  팀별 전체 로스터 — ASP.NET UpdatePanel 비동기 포스트백
  KboScheduler         위 크롤러들을 정해진 주기로 실행

web/         Thymeleaf 페이지 컨트롤러 (PageController, StandingsPageController, PlayerPageController)
*/  각 도메인 REST API 컨트롤러 (/api/**)
```

### 왜 크롤러가 여러 방식으로 나뉘어 있나

KBO 공식 사이트(koreabaseball.com)가 페이지마다 데이터를 내려주는 방식이 다 달라서,
페이지별로 맞는 방식을 따로 구현했습니다.

| 소스 | 방식 |
|---|---|
| 경기 일정(`Schedule.aspx`) | 페이지 자체는 빈 뼈대만 오고, 별도 AJAX(POST)가 JSON으로 실제 데이터를 줌 |
| 순위표/리더보드(영문 사이트) | 자바스크립트 없이 서버가 완성된 HTML을 바로 내려줌 (제일 간단) |
| 실시간 스코어보드 | 서버 렌더링, `lblGameState` 텍스트로 종료/진행중 구분 |
| 팀별 전체 로스터 | 옛날 방식 ASP.NET **UpdatePanel** — 자바스크립트가 숨겨진 필드를 채우고 비동기 포스트백을 보내면, 서버가 `<길이>|<타입>|<id>|<내용>|` 형식의 델타(변경분)만 돌려줌. 이 프로젝트에서 직접 델타 파서를 구현했습니다. |

## 실행 방법

### 1. 사전 준비
- Java 17
- MySQL 8

### 2. DB 준비
```sql
CREATE DATABASE scoremate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'scoremate'@'%' IDENTIFIED BY '원하는비밀번호';
GRANT ALL PRIVILEGES ON scoremate.* TO 'scoremate'@'%';
FLUSH PRIVILEGES;
```

### 3. 설정 파일
`src/main/resources/application.yml.example`을 복사해서 `application.yml`로 만들고,
DB 비밀번호와 JWT 시크릿 값을 채워주세요. (`application.yml`은 `.gitignore`에 포함되어 있어
실제 비밀번호가 커밋되지 않습니다.)

### 4. 실행
```bash
./gradlew bootRun
```
`http://localhost:8080` 접속.

앱이 시작되면 자동으로:
- 이전에 잘못 쌓인 중복 경기 데이터 정리
- 오늘 경기 일정 채우기
- (선택) `application.yml`에 `app.startup-sync.enabled: true`를 추가하면 시즌 전체 데이터까지 자동 백필

이후 새벽 시간대에 순위표/선수 기록/로스터가 자동으로 갱신되고, 진행중인 경기는 30초 주기로
실시간에 가깝게 업데이트됩니다.

## API

REST API도 `/api/**` 경로로 별도 제공합니다 (프론트를 다른 방식으로 붙이고 싶을 때 사용 가능).

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/matches?league=KBO&date=` | 날짜별 경기 조회 |
| GET | `/api/standings?league=KBO&season=` | 순위표 조회 |
| GET | `/api/teams?league=KBO` | 팀 목록 |
| GET | `/api/players/search?name=` | 선수 검색 |
| GET | `/api/players/{id}/records` | 선수 시즌 기록 |
| POST | `/api/users/signup` | 회원가입 |

## 테스트

실제 KBO 사이트로 네트워크 요청을 보내는 크롤러 테스트는 `src/test/.../test/` 패키지에 있고,
평소 `./gradlew test`에는 자동으로 안 걸리게 해뒀습니다 (클래스 상단 주석에 안내). 확인이
필요할 때 IntelliJ에서 해당 테스트 메서드를 직접 실행하면 됩니다.

네트워크 호출이 없는 순수 로직/페이지 테스트(`PageControllerTest` 등)는 평소 빌드에서
자동으로 같이 돕니다.

## 앞으로 할 것

- K리그 / 해외축구 5대리그 확장 (설계 단계에서 다중 리그를 고려해 `League` enum을 미리 분리해둠)
- 승부 예측 게임 / 직관 모임 매칭 (부가 기능)
- 팀별 전체 로스터 자동화 개선

## 저작권 관련 안내

이 프로젝트는 KBO 공식 사이트의 데이터를 개인 학습 목적으로 비상업적으로 크롤링합니다.
- 팀 로고/엠블럼 이미지는 사용하지 않음 (팀 컬러 + 텍스트 배지로 대체)
- 과도한 요청을 피하기 위해 크롤링 주기를 서버 부담이 적은 수준으로 제한
- 상업적 이용, 재배포 목적이 아닌 개인 프로젝트입니다
