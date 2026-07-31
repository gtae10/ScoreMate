import { useState } from 'react'
import MatchRow from './MatchRow.jsx'

export default function LeagueSection({ league }) {
  const [collapsed, setCollapsed] = useState(false)

  return (
    <div className={'league-card' + (collapsed ? ' collapsed' : '')}>
      <div className="league-head" onClick={() => setCollapsed(!collapsed)}>
        <div className="league-head-left">
          <div className="league-badge">{league.badge}</div>
          <div>
            <div className="league-name">{league.name}</div>
            <div className="league-sub">{league.sub}</div>
          </div>
        </div>
        <svg className="chevron" width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>

      <div className="match-list">
        {league.matches.map((match, i) => (
          <MatchRow key={i} match={match} />
        ))}
      </div>
    </div>
  )
}
