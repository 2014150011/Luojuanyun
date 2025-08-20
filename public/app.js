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
  const target = (location.hash || '#loader1').slice(1);
  switchTab(target);
}

tabButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    const tab = btn.dataset.tab;
    // Update URL to reflect navigation
    if (location.hash.slice(1) !== tab) {
      location.hash = `#${tab}`;
    } else {
      // If already on the same hash, still enforce UI state
      switchTab(tab);
    }
  });
});

window.addEventListener('hashchange', handleHashChange);
// Initialize on load
handleHashChange();

// Overlay logic for loader1 and loader2
const overlay = document.getElementById('overlay');
const overlayIframe = document.getElementById('overlay-iframe');
const btnExit = document.getElementById('btn-exit-fullscreen');

function openOverlay(url) {
  overlayIframe.src = url;
  overlay.classList.remove('hidden');
}

function closeOverlay() {
  overlay.classList.add('hidden');
  overlayIframe.src = 'about:blank';
}

btnExit.addEventListener('click', closeOverlay);

document.getElementById('btn-open-loader1').addEventListener('click', () => {
  const url = document.getElementById('select-loader1').value;
  openOverlay(url);
});

document.getElementById('btn-open-loader2').addEventListener('click', () => {
  const url = document.getElementById('select-loader2').value;
  openOverlay(url);
});

// Chat feature: mock Q&A with possible chart rendering
const chatWindow = document.getElementById('chat-window');
const chatForm = document.getElementById('chat-form');
const chatInput = document.getElementById('chat-input');

function appendMessage(role, content, chartSpec) {
  const wrapper = document.createElement('div');
  wrapper.className = `message msg-role-${role}`;

  const avatar = document.createElement('div');
  avatar.className = 'msg-avatar';
  avatar.textContent = role === 'user' ? 'U' : 'A';

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
    renderChartInMessage(canvas, chartSpec);
  }

  wrapper.appendChild(avatar);
  wrapper.appendChild(bubble);
  chatWindow.appendChild(wrapper);
  chatWindow.scrollTop = chatWindow.scrollHeight;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function generateAnswer(userText) {
  const lower = userText.toLowerCase();
  // If user asks for a chart, return a mock spec
  if (lower.includes('图') || lower.includes('chart') || lower.includes('柱状') || lower.includes('折线') || lower.includes('line') || lower.includes('bar')) {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const data = Array.from({ length: 7 }, () => randomInt(20, 120));
    const type = lower.includes('折线') || lower.includes('line') ? 'line' : 'bar';
    return {
      text: '这是根据你的描述生成的示例图表（随机数据）：',
      chartSpec: {
        type,
        data: {
          labels: days,
          datasets: [{
            label: '访问量',
            data,
            borderColor: '#6ea8fe',
            backgroundColor: 'rgba(110,168,254,0.35)'
          }]
        },
        options: {
          responsive: true,
          plugins: { legend: { labels: { color: '#e6eaf2' } } },
          scales: {
            x: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } },
            y: { ticks: { color: '#98a2b3' }, grid: { color: 'rgba(255,255,255,0.06)' } }
          }
        }
      }
    };
  }

  // Otherwise, simple echo with a helpful tip
  return { text: `已收到：${userText}\n你也可以要求我画一张柱状图/折线图来展示数据噢。` };
}

function renderChartInMessage(canvas, spec) {
  try {
    new Chart(canvas.getContext('2d'), spec);
  } catch (err) {
    console.error('Chart render error', err);
  }
}

chatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const value = chatInput.value.trim();
  if (!value) return;
  appendMessage('user', value);
  chatInput.value = '';

  // Simulate async response
  setTimeout(() => {
    const answer = generateAnswer(value);
    appendMessage('assistant', answer.text, answer.chartSpec);
  }, 400);
});

