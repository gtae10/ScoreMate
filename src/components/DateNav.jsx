import { useState } from 'react'

// 기준일. 나중엔 실제 오늘 날짜(new Date())로 바꾸면 됨 —
// 지금은 시안 확인용으로 프로젝트 진행 시점(2026-07-31)에 고정.
const TODAY = new Date(2026, 6, 31)
const WEEKDAY = ['일', '월', '화', '수', '목', '금', '토']

function addDays(date, days) {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

function labelFor(offset) {
  if (offset === 0) return '오늘'
  if (offset === -1) return '어제'
  if (offset === 1) return '내일'
  return WEEKDAY[addDays(TODAY, offset).getDay()]
}

function subLabelFor(offset) {
  const d = addDays(TODAY, offset)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

export default function DateNav({ selectedOffset, onSelect }) {
  const [windowCenter, setWindowCenter] = useState(0)

  const offsets = []
  for (let i = windowCenter - 4; i <= windowCenter + 4; i++) offsets.push(i)

  const showTodayJump = Math.abs(selectedOffset) > 3

  const jumpToToday = () => {
    onSelect(0)
    setWindowCenter(0)
  }

  return (
    <div className="date-nav">
      <button className="arrow-btn" title="일주일 전" onClick={() => setWindowCenter(windowCenter - 7)}>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
          <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <div className="date-tabs">
        {offsets.map((offset) => (
          <div
            key={offset}
            className={'date-tab' + (offset === selectedOffset ? ' active' : '')}
            onClick={() => onSelect(offset)}
          >
            {labelFor(offset)}
            <span className="sub">{subLabelFor(offset)}</span>
          </div>
        ))}
      </div>

      <button className="arrow-btn" title="일주일 후" onClick={() => setWindowCenter(windowCenter + 7)}>
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
          <path d="M9 6l6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <button
        className={'today-jump' + (showTodayJump ? ' show' : '')}
        onClick={jumpToToday}
      >
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
          <path d="M12 19V5M5 12l7-7 7 7" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        오늘로 이동
      </button>
    </div>
  )
}
