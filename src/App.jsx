import { useState } from 'react'
import TopBar from './components/TopBar.jsx'
import DateNav from './components/DateNav.jsx'
import SportNav from './components/SportNav.jsx'
import LeagueSection from './components/LeagueSection.jsx'
import { leagues } from './data/mockMatches.js'

export default function App() {
  const [selectedOffset, setSelectedOffset] = useState(0)
  const [selectedSport, setSelectedSport] = useState('baseball')

  const visibleLeagues = leagues.filter((l) => l.sport === selectedSport)

  return (
    <>
      <div className="header-stack">
        <TopBar />
        <DateNav selectedOffset={selectedOffset} onSelect={setSelectedOffset} />
      </div>

      <main>
        {visibleLeagues.map((league) => (
          <LeagueSection key={league.id} league={league} />
        ))}

        {visibleLeagues.length === 0 && (
          <div className="league-sub" style={{ textAlign: 'center', padding: '40px 0' }}>
            선택한 종목의 경기 정보가 아직 없어요.
          </div>
        )}

        <div className="section-toolbar">
          <button className="ghost-btn">
            더 많은 리그 보기
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </div>
      </main>

      <SportNav selectedSport={selectedSport} onSelect={setSelectedSport} />
    </>
  )
}
