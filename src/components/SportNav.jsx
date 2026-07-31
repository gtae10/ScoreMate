export default function SportNav({ selectedSport, onSelect }) {
  return (
    <div className="sport-nav">
      <div className="sport-nav-inner">
        <div
          className={'sport-tab' + (selectedSport === 'baseball' ? ' active' : '')}
          onClick={() => onSelect('baseball')}
        >
          <svg viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" />
            <path d="M7 5.5C9 8 9 16 7 18.5M17 5.5C15 8 15 16 17 18.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
          </svg>
          야구
        </div>
        <div
          className={'sport-tab' + (selectedSport === 'soccer' ? ' active' : '')}
          onClick={() => onSelect('soccer')}
        >
          <svg viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" />
            <path d="M12 8l3.5 2.5-1.3 4H9.8l-1.3-4L12 8z" stroke="currentColor" strokeWidth="1.4" strokeLinejoin="round" />
            <path d="M12 3.5v4.5M4.5 9l3.2 1.5M19.5 9l-3.2 1.5M8 19l1.8-4.5M16 19l-1.8-4.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
          </svg>
          축구
        </div>
      </div>
    </div>
  )
}
