const express = require('express');
const path = require('path');
const fs = require('fs');
const { execFile } = require('child_process');

const app = express();

const PORT = process.env.PORT || 5173;
const PUBLIC_DIR = path.join(__dirname, 'public');
const IMAGES_DIR = path.join(PUBLIC_DIR, 'images');

app.use(express.static(PUBLIC_DIR, {
  extensions: ['html'],
  setHeaders: (res) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
  }
}));

// Ensure images directory exists and pre-generate fund chart if missing
try {
  if (!fs.existsSync(IMAGES_DIR)) {
    fs.mkdirSync(IMAGES_DIR, { recursive: true });
    console.log('Created images directory at', IMAGES_DIR);
  }
  const fundPng = path.join(IMAGES_DIR, 'fund_nav.png');
  if (!fs.existsSync(fundPng)) {
    const scriptPath = path.join(__dirname, 'scripts', 'render_fund_chart.js');
    if (fs.existsSync(scriptPath)) {
      console.log('fund_nav.png not found. Rendering via script...');
      execFile(process.execPath, [scriptPath], { cwd: __dirname }, (err) => {
        if (err) {
          console.warn('Render fund chart failed:', err.message);
        } else {
          console.log('Rendered fund chart at', fundPng);
        }
      });
    }
  }
} catch (e) {
  console.warn('Image directory setup failed:', e.message);
}

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

