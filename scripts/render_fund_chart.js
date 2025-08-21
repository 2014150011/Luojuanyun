#!/usr/bin/env node
// Render an ECharts fund NAV chart to PNG and save to public/images/fund_nav.png
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');

(async () => {
  const outPath = path.join(__dirname, '..', 'public', 'images', 'fund_nav.png');
  const html = String.raw`<!doctype html>
  <html><head><meta charset="utf-8" />
  <style>
    html,body,#app{margin:0;padding:0;width:1200px;height:675px;background:#ffffff}
    body{font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, 'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;}
  </style>
  </head><body><div id="app"></div>
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
  <script>
  (function(){
    function genSeriesByDates(dates, base, vol){ const arr=[]; let v=base; for(let i=0;i<dates.length;i++){ v=v*(1+(Math.random()-0.5)*vol); if(v<0.6) v = 0.6 + Math.random()*0.05; arr.push(Number(v.toFixed(4))); } return arr; }
    function ma(data, w){ const out=[]; for(let i=0;i<data.length;i++){ const s=Math.max(0,i-w+1); const seg=data.slice(s,i+1); const m=seg.reduce((a,b)=>a+b,0)/seg.length; out.push(Number(m.toFixed(4))); } return out; }
    function daterange(start, end){ const out=[]; const s=new Date(start), e=new Date(end); for(let d=new Date(s); d<=e; d.setDate(d.getDate()+1)){ out.push(d.toISOString().slice(0,10)); } return out; }
    const productTitle = '上银理财“\u53cc周利”系列开放式(14天)理财产品WPTK24D1404期';
    const rangeText = '净值日期：2024-05-12 ~ 2025-05-27';
    const days = daterange('2024-05-12','2025-05-27');
    const nav = genSeriesByDates(days, 1.0000, 0.006);
    const ma20 = ma(nav, 20);
    const el=document.getElementById('app'); const chart=echarts.init(el);
    const gradient={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(37,99,235,0.25)'},{offset:1,color:'rgba(37,99,235,0.00)'}]};
    const avg=(nav.reduce((a,b)=>a+b,0)/nav.length).toFixed(4);
    const option={
      backgroundColor:'#ffffff',
      title:{ text: productTitle, left: 20, top: 10, textStyle:{ color:'#0f172a', fontSize:16, fontWeight:600 }, subtext: rangeText, subtextStyle:{ color:'#475569', fontSize:12 } },
      tooltip:{ trigger:'axis', axisPointer:{ type:'cross' } },
      grid:{ left:60,right:40,top:70,bottom:70 },
      legend:{ data:['单位净值 (NAV)','MA20'], top: 35, left: 20, textStyle:{ color:'#0f172a' } },
      dataZoom:[{type:'inside'},{type:'slider',height:16,bottom:20,backgroundColor:'#f1f5f9', borderColor:'#e2e8f0'}],
      xAxis:{ type:'category', data:days, axisLabel:{ color:'#475569', interval: 14 }, axisLine:{ lineStyle:{ color:'#cbd5e1' } }, axisTick:{ show:false } },
      yAxis:{ type:'value', name:'单位净值', nameTextStyle:{ color:'#475569' }, axisLabel:{ color:'#475569' }, splitLine:{ lineStyle:{ color:'#e2e8f0' } } },
      series:[
        { name:'单位净值 (NAV)', type:'line', data:nav, symbol:'none', smooth:true, lineStyle:{ color:'#2563eb', width:2 }, areaStyle:{ color:gradient }, markPoint:{ data:[{type:'max',name:'最高'},{type:'min',name:'最低'}] }, markLine:{ data:[{ name:'均值', yAxis:avg }] } },
        { name:'MA20', type:'line', data:ma20, symbol:'none', smooth:true, lineStyle:{ color:'#7c3aed', width:2, type:'dashed' } }
      ]
    };
    chart.setOption(option);
    window.chartReady=true;
  })();
  </script></body></html>`;

  const browser = await puppeteer.launch({ args: ['--no-sandbox','--disable-setuid-sandbox'] });
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 675, deviceScaleFactor: 2 });
  await page.setContent(html, { waitUntil: 'networkidle0' });
  await page.waitForFunction('window.chartReady === true');
  const element = await page.$('#app');
  const buffer = await element.screenshot({ type: 'png' });
  fs.writeFileSync(outPath, buffer);
  await browser.close();
  console.log('Saved chart to', outPath);
})();

