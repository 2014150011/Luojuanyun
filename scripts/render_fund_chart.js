#!/usr/bin/env node
// Render an ECharts fund NAV chart to PNG and save to public/images/fund_nav.png
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

(async () => {
  const outPath = path.join(__dirname, '..', 'public', 'images', 'fund_nav.png');
  const html = String.raw`<!doctype html>
  <html><head><meta charset="utf-8" />
  <style>html,body,#app{margin:0;padding:0;width:800px;height:480px;background:#0b1220}</style>
  </head><body><div id="app"></div>
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
  <script>
  (function(){
    function genSeries(n, base, vol){ const arr=[]; let v=base; for(let i=0;i<n;i++){ v=v*(1+(Math.random()-0.5)*vol); arr.push(Number(v.toFixed(4))); } return arr; }
    function ma(data, w){ const out=[]; for(let i=0;i<data.length;i++){ const s=Math.max(0,i-w+1); const seg=data.slice(s,i+1); const m=seg.reduce((a,b)=>a+b,0)/seg.length; out.push(Number(m.toFixed(4))); } return out; }
    const N=240; const days=Array.from({length:N}, (_,i)=>('D'+(i+1))); const nav=genSeries(N,1.0000,0.015); const ma20=ma(nav,20);
    const el=document.getElementById('app'); const chart=echarts.init(el);
    const gradient={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(110,168,254,0.45)'},{offset:1,color:'rgba(110,168,254,0.00)'}]};
    const avg=(nav.reduce((a,b)=>a+b,0)/nav.length).toFixed(4);
    const option={
      backgroundColor:'#0b1220',
      textStyle:{ color:'#e6eaf2' },
      tooltip:{ trigger:'axis', axisPointer:{ type:'cross' } },
      grid:{ left:50,right:30,top:30,bottom:40 },
      legend:{ data:['单位净值 (NAV)','MA20'], textStyle:{ color:'#dbeafe' } },
      dataZoom:[{type:'inside'},{type:'slider',height:16,bottom:10,backgroundColor:'rgba(255,255,255,0.06)'}],
      xAxis:{ type:'category', data:days, axisLabel:{ color:'#94a3b8' }, axisLine:{ lineStyle:{ color:'#334155' } } },
      yAxis:{ type:'value', axisLabel:{ color:'#94a3b8' }, splitLine:{ lineStyle:{ color:'rgba(148,163,184,0.18)' } } },
      series:[
        { name:'单位净值 (NAV)', type:'line', data:nav, symbol:'none', smooth:true, lineStyle:{ color:'#60a5fa', width:2 }, areaStyle:{ color:gradient }, markPoint:{ data:[{type:'max',name:'最高'},{type:'min',name:'最低'}] }, markLine:{ data:[{ name:'均值', yAxis:avg }] } },
        { name:'MA20', type:'line', data:ma20, symbol:'none', smooth:true, lineStyle:{ color:'#a78bfa', width:2, type:'dashed' } }
      ]
    };
    chart.setOption(option);
    window.chartReady=true;
  })();
  </script></body></html>`;

  const browser = await puppeteer.launch({ args: ['--no-sandbox','--disable-setuid-sandbox'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 800, height: 480, deviceScaleFactor: 2 });
  await page.setContent(html, { waitUntil: 'networkidle0' });
  await page.waitForFunction('window.chartReady === true');
  const element = await page.$('#app');
  const buffer = await element.screenshot({ type: 'png' });
  fs.writeFileSync(outPath, buffer);
  await browser.close();
  console.log('Saved chart to', outPath);
})();

