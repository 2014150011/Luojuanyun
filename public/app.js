// Tab switching with hash routing
const tabButtons = Array.from(document.querySelectorAll('.tab'));
const panels = {
  loader1: document.getElementById('panel-loader1'),
  chat: document.getElementById('panel-chat'),
  loader2: document.getElementById('panel-loader2')
};

const tabKeyToButton = new Map(tabButtons.map((btn) => [btn.dataset.tab, btn]));

function switchTab(tabKey) {
  const key = panels[tabKey] ? tabKey : 'loader1';
  // buttons state
  tabButtons.forEach((b) => {
    const isActive = b.dataset.tab === key;
    b.classList.toggle('active', isActive);
    b.setAttribute('aria-selected', String(isActive));
  });
  // panels state
  Object.entries(panels).forEach(([k, panel]) => {
    panel.classList.toggle('hidden', k !== key);
  });
}

function handleHashChange() {
  // Auto-close overlay when navigating between tabs (safe even before overlay vars are initialized)
  const overlayEl = document.getElementById('overlay');
  if (overlayEl && !overlayEl.classList.contains('hidden')) {
    overlayEl.classList.add('hidden');
    const iframeEl = document.getElementById('overlay-iframe');
    if (iframeEl) iframeEl.src = 'about:blank';
  }
  const target = (location.hash || '#loader1').slice(1);
  switchTab(target);
}

tabButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    const tab = btn.dataset.tab;
    // For <a> elements, allow default hash navigation, but still normalize state
    setTimeout(() => switchTab(tab), 0);
  });
});

window.addEventListener('hashchange', handleHashChange);
// Initialize on load; if no hash, set default
if (!location.hash) {
  location.replace('#loader1');
}
handleHashChange();

// Overlay logic for loader1 and loader2
const overlay = document.getElementById('overlay');
const overlayIframe = document.getElementById('overlay-iframe');
const btnExit = document.getElementById('btn-exit-fullscreen');
const appHeader = document.querySelector('.app-header');
const overlayLoading = document.getElementById('overlay-loading');

function updateOverlayOffset() {
  const headerHeight = appHeader ? appHeader.offsetHeight : 0;
  if (overlay && overlay.style && typeof overlay.style.setProperty === 'function') {
    overlay.style.setProperty('--overlay-top', `${headerHeight}px`);
  }
}

function isFullscreenActive() {
  return document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement;
}

function requestFullscreen(el) {
  if (!el) return;
  try {
    if (el.requestFullscreen) return el.requestFullscreen();
    if (el.webkitRequestFullscreen) return el.webkitRequestFullscreen();
    if (el.mozRequestFullScreen) return el.mozRequestFullScreen();
    if (el.msRequestFullscreen) return el.msRequestFullscreen();
  } catch (_) {
    // ignore
  }
}

function exitFullscreen() {
  try {
    if (document.exitFullscreen) return document.exitFullscreen();
    if (document.webkitExitFullscreen) return document.webkitExitFullscreen();
    if (document.mozCancelFullScreen) return document.mozCancelFullScreen();
    if (document.msExitFullscreen) return document.msExitFullscreen();
  } catch (_) {
    // ignore
  }
}

function openOverlay(url) {
  if (!overlay || !overlayIframe) return;
  if (overlayLoading) overlayLoading.classList.remove('hidden');
  updateOverlayOffset();
  // delay 3s then show iframe and load
  setTimeout(() => {
    overlayIframe.src = url;
    if (overlayLoading) overlayLoading.classList.add('hidden');
    overlay.classList.remove('hidden');
    // Try to enter real fullscreen for better UX
    requestFullscreen(overlay);
  }, 3000);
}

function closeOverlay() {
  if (!overlay || !overlayIframe) return;
  overlay.classList.add('hidden');
  overlayIframe.src = 'about:blank';
  if (overlayLoading) overlayLoading.classList.add('hidden');
  // Exit real fullscreen if active
  if (isFullscreenActive()) {
    exitFullscreen();
  }
}

if (btnExit) btnExit.addEventListener('click', closeOverlay);

// Close overlay when clicking on the dim background (not toolbar/iframe)
if (overlay) {
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) {
      closeOverlay();
    }
  });
}

// Close with Esc
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && overlay && !overlay.classList.contains('hidden')) {
    closeOverlay();
  }
});

// Close overlay when clicking top tabs
const tabsNav = document.querySelector('.tabs');
if (tabsNav) {
  tabsNav.addEventListener('click', (e) => {
    const el = e.target;
    if (el && el.classList && el.classList.contains('tab') && !overlay.classList.contains('hidden')) {
      closeOverlay();
    }
  });
}

// Recalculate overlay offset on resize while open
window.addEventListener('resize', () => {
  if (overlay && !overlay.classList.contains('hidden')) {
    updateOverlayOffset();
  }
});

const btnOpen1 = document.getElementById('btn-open-loader1');
if (btnOpen1) {
  btnOpen1.addEventListener('click', () => {
    const sel = document.getElementById('select-loader1');
    const url = sel ? sel.value : '';
    if (url) openOverlay(url);
  });
}

const btnOpen2 = document.getElementById('btn-open-loader2');
if (btnOpen2) {
  btnOpen2.addEventListener('click', () => {
    const sel = document.getElementById('select-loader2');
    const url = sel ? sel.value : '';
    if (url) openOverlay(url);
  });
}

// Chat feature with presets and typing indicator
const chatWindow = document.getElementById('chat-window');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');
const chatPresets = document.querySelectorAll('.preset');

function appendMessage(role, content, chartSpec, tableSpec, imageSpec, listSpec) {
  // Create or reuse the last exchange block
  let exchange = chatWindow.lastElementChild;
  if (!exchange || !exchange.classList.contains('exchange') || (role === 'user' && exchange.dataset.hasUser === 'true') || (role === 'assistant' && exchange.dataset.hasAssistant === 'true')) {
    exchange = document.createElement('div');
    exchange.className = 'exchange';
    exchange.dataset.hasUser = 'false';
    exchange.dataset.hasAssistant = 'false';
    chatWindow.appendChild(exchange);
  }

  const wrapper = document.createElement('div');
  wrapper.className = `message msg-role-${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'msg-avatar';
  const img = document.createElement('img');
  img.className = 'msg-avatar-img';
  img.alt = role === 'user' ? '用户' : '助手';
  img.src = role === 'user' ? 'images/user.svg' : 'images/assistant.svg';
  avatar.appendChild(img);

  const bubble = document.createElement('div');
  bubble.className = 'msg-bubble';

  const text = document.createElement('div');
  text.className = 'msg-content';
  text.textContent = content;
  bubble.appendChild(text);

  if (chartSpec) {
    const chartContainer = document.createElement('div');
    chartContainer.className = 'msg-chart';
    const canvas = document.createElement('canvas');
    chartContainer.appendChild(canvas);
    bubble.appendChild(chartContainer);
    requestAnimationFrame(() => renderChartInMessage(canvas, chartSpec));
  }

  if (tableSpec) {
    const tableContainer = document.createElement('div');
    tableContainer.className = 'msg-table';
    renderTableInMessage(tableContainer, tableSpec);
    bubble.appendChild(tableContainer);
  }

  if (imageSpec) {
    const imageContainer = document.createElement('div');
    imageContainer.className = 'msg-image';
    renderImageInMessage(imageContainer, imageSpec);
    bubble.appendChild(imageContainer);
  }

  if (listSpec && Array.isArray(listSpec) && listSpec.length) {
    const list = document.createElement('div');
    list.style.marginTop = '8px';
    listSpec.forEach((item) => {
      const row = document.createElement('div');
      row.style.display = 'flex';
      row.style.gap = '8px';
      const marker = document.createElement('strong');
      marker.textContent = '>';
      const textNode = document.createElement('div');
      textNode.textContent = item;
      row.appendChild(marker);
      row.appendChild(textNode);
      list.appendChild(row);
    });
    bubble.appendChild(list);
  }

  wrapper.appendChild(avatar);
  wrapper.appendChild(bubble);
  exchange.appendChild(wrapper);
  if (role === 'user') exchange.dataset.hasUser = 'true';
  if (role === 'assistant') exchange.dataset.hasAssistant = 'true';
  chatWindow.scrollTop = chatWindow.scrollHeight;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// Predefined Q&A map (tracking/埋点可在此扩展)
const presetAnswers = {
  '查看最近7天访问量（柱状图）': { text: '这是最近7天访问量的柱状图：', type: 'bar', aliases: ['7天 访问量 柱状', '七天 柱状 图'] },
  '查看最近7天访问量（折线图）': { text: '这是最近7天访问量的折线图：', type: 'line', aliases: ['7天 访问量 折线', '七天 折线 图'] },
  '查看渠道表现表格': {
    text: '这是渠道表现表格：',
    aliases: ['渠道 表格', '渠道 表现', '渠道 数据'],
    table: {
      headers: ['渠道', '新增用户', '次日留存', '7日留存', '客单价'],
      rows: [
        ['渠道 A', '24,310', '34.2%', '18.9%', '¥ 46.3'],
        ['渠道 B', '18,905', '31.1%', '15.4%', '¥ 41.2'],
        ['自然流量', '12,770', '39.5%', '21.7%', '¥ 35.8']
      ]
    }
  },
  '查看产品结构（图片）': {
    text: '这是产品结构图（示意）：',
    aliases: ['产品 结构', '产品 图片', '结构 图片'],
    image: { src: 'images/complex-diagram.svg', alt: '产品结构示意', caption: '系统架构与数据流总览' }
  },
  '输出关键结论要点（纯文本）': {
    text: '以下为关键结论要点：',
    aliases: ['关键 结论', '结论 要点'],
    list: [
      '增长主要来自自然流量与渠道A',
      '复购提升与产品结构优化相关',
      '建议扩大灰度并加强用户引导'
    ]
  }
};

function findPresetByQuestion(userText) {
  const key = Object.keys(presetAnswers).find(k => userText.includes(k));
  if (key) return presetAnswers[key];
  for (const k of Object.keys(presetAnswers)) {
    const cfg = presetAnswers[k];
    if (cfg.aliases && cfg.aliases.some(alias => userText.includes(alias))) {
      return cfg;
    }
  }
  return null;
}

function generateAnswer(userText) {
  const lower = userText.toLowerCase();
  // Priority: preset answers
  const preset = findPresetByQuestion(userText);
  if (preset) {
    if (preset.type === 'bar' || preset.type === 'line') {
      const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
      const data = Array.from({ length: 7 }, () => randomInt(20, 120));
      return {
        text: preset.text,
        chartSpec: {
          type: preset.type,
          data: { labels: days, datasets: [{ label: '访问量', data, borderColor: '#6ea8fe', backgroundColor: preset.type === 'line' ? 'transparent' : 'rgba(110,168,254,0.35)', fill: preset.type === 'line' ? false : true, tension: 0.35, pointRadius: preset.type === 'line' ? 3 : 0, pointBackgroundColor: '#6ea8fe' }] },
          options: { responsive: true, maintainAspectRatio: false, animation: { duration: 300 }, plugins: { legend: { labels: { color: '#e6eaf2' } } }, scales: { x: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } }, y: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } } } }
        },
        tableSpec: preset.table,
        imageSpec: preset.image,
        listSpec: preset.list
      };
    }
    return { text: preset.text, tableSpec: preset.table, imageSpec: preset.image, listSpec: preset.list };
  }

  // Fallback: general chart intent detection
  if (lower.includes('图') || lower.includes('chart') || lower.includes('柱状') || lower.includes('折线') || lower.includes('line') || lower.includes('bar')) {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const data = Array.from({ length: 7 }, () => randomInt(20, 120));
    const type = lower.includes('折线') || lower.includes('line') ? 'line' : 'bar';
    return {
      text: type === 'line' ? '这是折线图：' : '这是柱状图：',
      chartSpec: { type, data: { labels: days, datasets: [{ label: '访问量', data, borderColor: '#6ea8fe', backgroundColor: type === 'line' ? 'transparent' : 'rgba(110,168,254,0.35)', fill: type === 'line' ? false : true, tension: 0.35, pointRadius: type === 'line' ? 3 : 0, pointBackgroundColor: '#6ea8fe' }] }, options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { labels: { color: '#e6eaf2' } } }, scales: { x: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } }, y: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } } } } },
      listSpec: []
    };
  }

  // Fallback preset for non-chart known phrases could be added above; keep minimal text
  return { text: '好的。' };
}

function renderChartInMessage(canvas, spec) {
  try {
    const ctx = canvas.getContext('2d');
    const merged = {
      type: spec.type || 'bar',
      data: spec.data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#e6eaf2' } } },
        scales: {
          x: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } },
          y: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } }
        },
        ...(spec.options || {})
      }
    };
    new Chart(ctx, merged);
  } catch (err) {
    console.error('Chart render error', err);
  }
}

function renderTableInMessage(container, tableSpec) {
  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const trHead = document.createElement('tr');
  (tableSpec.headers || []).forEach((h) => {
    const th = document.createElement('th');
    th.textContent = h;
    trHead.appendChild(th);
  });
  thead.appendChild(trHead);
  table.appendChild(thead);

  const tbody = document.createElement('tbody');
  (tableSpec.rows || []).forEach((row) => {
    const tr = document.createElement('tr');
    row.forEach((cell) => {
      const td = document.createElement('td');
      td.textContent = cell;
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
  table.appendChild(tbody);
  container.appendChild(table);
}

function renderImageInMessage(container, imageSpec) {
  const img = document.createElement('img');
  img.src = imageSpec.src;
  img.alt = imageSpec.alt || '';
  img.style.maxWidth = '560px';
  img.style.borderRadius = '10px';
  img.style.border = '1px solid #2b3a5a';
  container.appendChild(img);
  if (imageSpec.caption) {
    const cap = document.createElement('div');
    cap.className = 'msg-caption';
    cap.textContent = imageSpec.caption;
    container.appendChild(cap);
  }
}

if (chatForm && chatInput) {
  chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const value = chatInput.value.trim();
    if (!value) return;
    appendMessage('user', value);
    chatInput.value = '';

    const typing = document.createElement('div');
    typing.className = 'message msg-role-assistant typing';
    typing.innerHTML = '<div class="msg-avatar"><img class="msg-avatar-img" src="images/assistant.svg" alt="助手"/></div><div class="msg-bubble"><div class="msg-content"><span class="spinner"></span> 正在思考…</div></div>';
    const ex = document.createElement('div');
    ex.className = 'exchange';
    ex.dataset.hasUser = 'true';
    ex.dataset.hasAssistant = 'false';
    chatWindow.appendChild(ex);
    ex.appendChild(typing);
    chatWindow.scrollTop = chatWindow.scrollHeight;

    setTimeout(() => {
      const answer = generateAnswer(value);
      typing.remove();
      appendMessage('assistant', answer.text, answer.chartSpec, answer.tableSpec, answer.imageSpec, answer.listSpec);
    }, 3000);
  });
}

if (chatPresets && chatPresets.length) {
  chatPresets.forEach((btn) => {
    btn.addEventListener('click', () => {
      const q = btn.getAttribute('data-q') || '';
      if (!q) return;
      appendMessage('user', q);

      const typing = document.createElement('div');
      typing.className = 'message msg-role-assistant typing';
      typing.innerHTML = '<div class="msg-avatar"><img class="msg-avatar-img" src="images/assistant.svg" alt="助手"/></div><div class="msg-bubble"><div class="msg-content"><span class="spinner"></span> 正在思考…</div></div>';
      const ex = document.createElement('div');
      ex.className = 'exchange';
      ex.dataset.hasUser = 'true';
      ex.dataset.hasAssistant = 'false';
      chatWindow.appendChild(ex);
      ex.appendChild(typing);
      chatWindow.scrollTop = chatWindow.scrollHeight;

      setTimeout(() => {
        const answer = generateAnswer(q);
        typing.remove();
        appendMessage('assistant', answer.text, answer.chartSpec, answer.tableSpec, answer.imageSpec, answer.listSpec);
      }, 3000);
    });
  });
}