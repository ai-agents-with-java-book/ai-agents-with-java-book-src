import './style.css'

const grid = document.getElementById('tickets-grid')
const emptyState = document.getElementById('empty-state')
const statusDot = document.getElementById('status-dot')
const statusLabel = document.getElementById('status-label')

// ── Tab switching ────────────────────────────────────────────────────────────
const tabButtons = document.querySelectorAll('.tab-btn')
const tabPanels  = document.querySelectorAll('.tab-panel')

tabButtons.forEach(btn => {
  btn.addEventListener('click', () => {
    tabButtons.forEach(b => {
      b.classList.remove('tab-active')
      b.setAttribute('aria-selected', 'false')
    })
    tabPanels.forEach(p => p.classList.add('tab-panel-hidden'))

    btn.classList.add('tab-active')
    btn.setAttribute('aria-selected', 'true')

    const target = document.getElementById(btn.getAttribute('aria-controls'))
    target.classList.remove('tab-panel-hidden')

    // Lazy-load the recent panel on first activation
    if (btn.id === 'tab-recent' && !btn.dataset.loaded) {
      btn.dataset.loaded = 'true'
      loadRecentIncidences()
    }
  })
})

// ── Refresh button ───────────────────────────────────────────────────────────
document.getElementById('btn-refresh').addEventListener('click', loadRecentIncidences)

// ── Sentiment badge helper ───────────────────────────────────────────────────
function sentimentBadge(sentiment) {
  if (!sentiment) return ''
  const cls = sentiment.result === 'POSITIVE' ? 'badge-positive'
    : sentiment.result === 'NEGATIVE' ? 'badge-negative'
    : 'badge-neutral'
  return `<span class="badge ${cls}">${sentiment.result}</span>`
}

// ── Render a single ticket card ──────────────────────────────────────────────
function renderTicket(data) {
  const { instanceId, request, response } = data
  const cardId = `ticket-${instanceId}`

  // Remove existing card with same instanceId (update case)
  const existing = document.getElementById(cardId)
  if (existing) existing.remove()

  const card = document.createElement('article')
  card.className = 'ticket-card'
  card.id = cardId

  card.innerHTML = `
    <div class="ticket-header">
      <div class="ticket-meta">
        <span class="ticket-user">${escHtml(request.user)}</span>
        <a class="ticket-email" href="mailto:${escHtml(request.email)}">${escHtml(request.email)}</a>
        <span class="ticket-booking">Booking #${escHtml(String(request.bookingId))}</span>
      </div>
      <div class="ticket-badges">
        ${response.sentiment ? sentimentBadge(response.sentiment) : ''}
      </div>
    </div>

    <div class="ticket-section">
      <label class="section-label">Customer Message</label>
      <p class="customer-message">${escHtml(request.message)}</p>
    </div>

    ${response.sentiment?.reason ? `
    <div class="ticket-section">
      <label class="section-label">Sentiment Analysis</label>
      <p class="sentiment-reason">${escHtml(response.sentiment.reason)}</p>
    </div>` : ''}

    <div class="ticket-section">
      <label class="section-label" for="draft-${escHtml(instanceId)}">Response Draft <span class="editable-hint">(editable)</span></label>
      <textarea
        id="draft-${escHtml(instanceId)}"
        class="draft-editor"
        rows="5"
        placeholder="Edit the response draft…"
      >${escHtml(response.draft)}</textarea>
    </div>

    <div class="ticket-actions">
      <span class="send-feedback" id="feedback-${escHtml(instanceId)}"></span>
      <button class="btn-send" data-instance-id="${escHtml(instanceId)}">
        <svg viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M2.94 2.06a1 1 0 0 1 1.06-.22l14 6a1 1 0 0 1 0 1.83l-14 6a1 1 0 0 1-1.4-1.16L4.54 10 2.6 3.49a1 1 0 0 1 .34-1.43z"/></svg>
        Send Response
      </button>
    </div>
  `

  // Prepend so newest tickets appear at top
  grid.prepend(card)

  if (emptyState.style.display !== 'none') {
    emptyState.style.display = 'none'
  }

  // Wire send button
  card.querySelector('.btn-send').addEventListener('click', () => sendTicket(instanceId, card))
}

// ── Send updated draft to backend ────────────────────────────────────────────
async function sendTicket(instanceId, card) {
  const textarea = card.querySelector('.draft-editor')
  const feedback = card.querySelector('.send-feedback')
  const btn = card.querySelector('.btn-send')
  const draft = textarea.value.trim()

  if (!draft) {
    showFeedback(feedback, 'Draft cannot be empty.', 'error')
    return
  }

  btn.disabled = true
  btn.textContent = 'Sending…'

  try {
    const res = await fetch('/ticket/update', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Flow-Instance-Id': instanceId,
      },
      body: JSON.stringify({ draft }),
    })

    if (res.ok || res.status === 202) {
      showFeedback(feedback, 'Response transmitted successfully.', 'success')
      card.classList.add('card-sent')
    } else {
      showFeedback(feedback, `Error ${res.status}: ${res.statusText}`, 'error')
    }
  } catch (err) {
    showFeedback(feedback, `Network error: ${err.message}`, 'error')
  } finally {
    btn.disabled = false
    btn.innerHTML = `<svg viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M2.94 2.06a1 1 0 0 1 1.06-.22l14 6a1 1 0 0 1 0 1.83l-14 6a1 1 0 0 1-1.4-1.16L4.54 10 2.6 3.49a1 1 0 0 1 .34-1.43z"/></svg> Send Response`
  }
}

function showFeedback(el, message, type) {
  el.textContent = message
  el.className = `send-feedback feedback-${type}`
  setTimeout(() => { el.textContent = ''; el.className = 'send-feedback' }, 4000)
}

// ── Escape HTML entities ─────────────────────────────────────────────────────
function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// ── Recent incidences ─────────────────────────────────────────────────────────
async function loadRecentIncidences() {
  const list    = document.getElementById('incidences-list')
  const loading = document.getElementById('recent-loading')
  const empty   = document.getElementById('recent-empty')

  list.innerHTML = ''
  empty.style.display = 'none'
  loading.style.display = 'flex'

  try {
    const res = await fetch('/ticket/recent')
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const incidences = await res.json()

    loading.style.display = 'none'

    if (!incidences.length) {
      empty.style.display = 'flex'
      return
    }

    incidences.forEach(inc => list.appendChild(renderIncidenceRow(inc)))
  } catch (err) {
    loading.style.display = 'none'
    list.innerHTML = `<p class="recent-fetch-error">Failed to load messages: ${escHtml(err.message)}</p>`
  }
}

function renderIncidenceRow(inc) {
  const row = document.createElement('article')
  row.className = 'incidence-row'

  const date = inc.createdAt
    ? new Date(inc.createdAt).toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: 'numeric' })
    : '—'

  row.innerHTML = `
    <div class="incidence-meta">
      <span class="incidence-date">${escHtml(date)}</span>
      ${inc.bookingId ? `<span class="ticket-booking">Booking #${escHtml(String(inc.bookingId))}</span>` : ''}
      ${inc.userEmail ? `<a class="ticket-email" href="mailto:${escHtml(inc.userEmail)}">${escHtml(inc.userEmail)}</a>` : ''}
    </div>
    <div class="incidence-body">
      <div class="incidence-col">
        <label class="section-label">Customer Message</label>
        <p class="customer-message">${escHtml(inc.message ?? '—')}</p>
      </div>
      ${inc.response ? `
      <div class="incidence-col">
        <label class="section-label">Response Sent</label>
        <p class="incidence-response">${escHtml(inc.response)}</p>
      </div>` : ''}
    </div>
  `
  return row
}

// ── SSE connection with auto-reconnect ───────────────────────────────────────
function connect() {
  const es = new EventSource('/ticket/hil')

  es.addEventListener('open', () => {
    statusDot.className = 'status-dot dot-connected'
    statusLabel.textContent = 'Live — connected to mission control'
  })

  es.addEventListener('message', (event) => {
    try {
      const data = JSON.parse(event.data)
      renderTicket(data)
    } catch (err) {
      console.error('Failed to parse SSE message:', err, event.data)
    }
  })

  es.addEventListener('error', () => {
    statusDot.className = 'status-dot dot-disconnected'
    statusLabel.textContent = 'Disconnected — retrying…'
    es.close()
    setTimeout(connect, 3000)
  })
}

connect()
