export default function TopBar() {
  return (
    <div className="topbar">
      <div className="logo">
        <div className="logo-mark">SM</div>
        ScoreMate
      </div>

      <nav className="primary-nav">
        <a className="active">경기</a>
        <a>순위</a>
        <a>선수</a>
        <a>즐겨찾기</a>
      </nav>

      <div className="top-actions">
        <button className="icon-btn" title="검색">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
            <path d="M21 21L16.65 16.65" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  )
}
