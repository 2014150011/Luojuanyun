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
    body{font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;}
  </style>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;600&display=swap" rel="stylesheet">
  </head><body><div id="app"></div>
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
  <script>
  (function(){
    function genSeriesByDates(dates, base, vol){ const arr=[]; let v=base; for(let i=0;i<dates.length;i++){ v=v*(1+(Math.random()-0.5)*vol); if(v<0.6) v = 0.6 + Math.random()*0.05; arr.push(Number(v.toFixed(4))); } return arr; }
    function ma(data, w){ const out=[]; for(let i=0;i<data.length;i++){ const s=Math.max(0,i-w+1); const seg=data.slice(s,i+1); const m=seg.reduce((a,b)=>a+b,0)/seg.length; out.push(Number(m.toFixed(4))); } return out; }
    function daterange(start, end){ const out=[]; const s=new Date(start), e=new Date(end); for(let d=new Date(s); d<=e; d.setDate(d.getDate()+1)){ out.push(d.toISOString().slice(0,10)); } return out; }
    // 使用完整日期作为类目，配合 formatter 仅在每月首日显示 YYYY-MM
    const productTitle = '上银理财“双周利”系列开放式(14天)理财产品WPTK24D1404期';
    const rangeText = '净值日期：2024-05-12 ~ 2025-05-27';
    const days = daterange('2024-05-12','2025-05-27');
    const nav = genSeriesByDates(days, 1.0000, 0.006);
    const ma20 = ma(nav, 20);
    const el=document.getElementById('app'); const chart=echarts.init(el);
    const faintArea={type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(31,58,138,0.06)'},{offset:1,color:'rgba(31,58,138,0.00)'}]};
    const avg=(nav.reduce((a,b)=>a+b,0)/nav.length).toFixed(4);
    const option={
      backgroundColor:'#ffffff',
      title:{ text: productTitle, left: 20, top: 10, textStyle:{ color:'#0f172a', fontSize:16, fontWeight:600 }, subtext: rangeText, subtextStyle:{ color:'#475569', fontSize:12 } },
      tooltip:{ trigger:'axis', axisPointer:{ type:'cross' } },
      grid:{ left:60,right:40,top:70,bottom:70 },
      legend:{ data:['单位净值 (NAV)','MA20'], top: 20, right: 20, textStyle:{ color:'#111827' }, icon:'roundRect' },
      dataZoom:[{type:'inside'},{type:'slider',height:16,bottom:20,backgroundColor:'#f1f5f9', borderColor:'#e2e8f0'}],
      xAxis:{ type:'category', data: days, boundaryGap: true,
        axisLabel:{ color:'#374151', interval: 0, rotate: 0, margin: 12, hideOverlap: true,
          showMinLabel: false, showMaxLabel: true,
          formatter: function(value, idx){ return value && value.slice(8,10)==='01' ? value.slice(0,7) : ''; }
        },
        axisLine:{ lineStyle:{ color:'#cbd5e1' } }, axisTick:{ show:false } },
      yAxis:{ type:'value', name:'单位净值', nameTextStyle:{ color:'#374151' }, axisLabel:{ color:'#374151', formatter:(val)=> Number(val).toFixed(4) }, splitLine:{ lineStyle:{ color:'#e5e7eb' } } },
      series:[
        { name:'单位净值 (NAV)', type:'line', data:nav, symbol:'none', smooth:true, lineStyle:{ color:'#1f3a8a', width:2 }, areaStyle:{ color:faintArea }, 
          markPoint:{ data:[ { coord:[days[days.length-1], nav[nav.length-1]], value: nav[nav.length-1], name:'latest', itemStyle:{ color:'#1f3a8a' }, label:{ formatter:function(p){ return '最新: ' + Number(p.value).toFixed(4); }, color:'#111827', backgroundColor:'#f3f4f6', borderColor:'#e5e7eb', borderWidth:1, padding:[2,6], borderRadius:3 } } ] },
          markLine:{ data:[{ name:'均值', yAxis:avg }], lineStyle:{ type:'dashed', color:'#9ca3af' }, label:{ color:'#6b7280' } }
        },
        { name:'MA20', type:'line', data:ma20, symbol:'none', smooth:true, lineStyle:{ color:'#6b7280', width:2, type:'dashed' } }
      ]
    };
    chart.setOption(option);
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function(){ chart.resize(); window.chartReady = true; });
    } else {
      window.chartReady = true;
    }
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

