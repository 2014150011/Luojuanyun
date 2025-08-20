const express = require('express');
const path = require('path');

const app = express();

const PORT = process.env.PORT || 5173;
const PUBLIC_DIR = path.join(__dirname, 'public');

app.use(express.static(PUBLIC_DIR, {
  extensions: ['html'],
  setHeaders: (res) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
  }
}));

// Serve local Chart.js for offline/robust chart rendering
let chartUmdPath = null;
try {
  chartUmdPath = require.resolve('chart.js/dist/chart.umd.js');
  app.get('/vendor/chart.umd.js', (_req, res) => {
    res.sendFile(chartUmdPath);
  });
} catch (_) {
  // If not installed, the CDN fallback in index.html will be used
}

// Fallback to index.html for root
app.get('/', (_req, res) => {
  res.sendFile(path.join(PUBLIC_DIR, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
});

