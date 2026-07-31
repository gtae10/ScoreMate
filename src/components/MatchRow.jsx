export default function MatchRow({ match }) {
  const { status, live, home, away, homeScore, awayScore } = match
  const hasScore = homeScore !== null && homeScore !== undefined

  return (
    <div className="match-row">
      <div className={'status-badge' + (live ? ' live' : '')}>{status}</div>

      <div className="team home">
        <div className="crest" style={{ background: home.color }}>{home.short}</div>
        {home.name}
      </div>

      <div className="score-box">
        {hasScore ? (
          <>
            <span className={'n' + (homeScore < awayScore ? ' lose' : '')}>{homeScore}</span>
            <span className="dash">–</span>
            <span className={'n' + (awayScore < homeScore ? ' lose' : '')}>{awayScore}</span>
          </>
        ) : (
          <span className="dash">– : –</span>
        )}
      </div>

      <div className="team away">
        <div className="crest" style={{ background: away.color }}>{away.short}</div>
        {away.name}
      </div>
    </div>
  )
}
