// TODO: 나중에 이 파일 대신 실제 백엔드 API(GET /api/matches 등)에서 받아온
// 데이터를 쓰도록 교체. 지금은 화면 구조/스타일 확인용 목업 데이터.

export const leagues = [
  {
    id: 'kbo',
    name: 'KBO 리그',
    sport: 'baseball',
    badge: 'KBO',
    sub: '2026 정규시즌 · 7월 1일',
    matches: [
      { status: 'FT', home: { name: '롯데', short: 'LT', color: '#c8102e' }, away: { name: '두산', short: '두산', color: '#131230' }, homeScore: 5, awayScore: 2 },
      { status: 'FT', home: { name: 'SSG', short: 'SSG', color: '#ce0e2d' }, away: { name: 'KIA', short: 'KIA', color: '#ea0029' }, homeScore: 6, awayScore: 6 },
      { status: 'FT', home: { name: '삼성', short: '삼성', color: '#074ca1' }, away: { name: 'NC', short: 'NC', color: '#315288' }, homeScore: 5, awayScore: 10 },
      { status: 'FT', home: { name: 'KT', short: 'KT', color: '#000000' }, away: { name: '한화', short: '한화', color: '#fc4e00' }, homeScore: 7, awayScore: 4 },
      { status: 'FT', home: { name: 'LG', short: 'LG', color: '#a90031' }, away: { name: '키움', short: '키움', color: '#570514' }, homeScore: 10, awayScore: 4 },
    ],
  },
  {
    id: 'kleague',
    name: 'K리그1',
    sport: 'soccer',
    badge: 'KL',
    sub: '2026 · 22라운드',
    matches: [
      { status: '67\'', live: true, home: { name: '울산HD', short: '울산', color: '#00358e' }, away: { name: '포항', short: '포항', color: '#005bac' }, homeScore: 1, awayScore: 1 },
      { status: '19:30', home: { name: 'FC서울', short: '서울', color: '#e4032e' }, away: { name: '전북', short: '전북', color: '#004b93' }, homeScore: null, awayScore: null },
    ],
  },
]
