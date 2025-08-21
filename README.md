## 本地前端工程

这个项目是一个可本地运行的前端工程，包含三个功能：
- 功能1：通过下拉框选择并加载一个 HTML 文件，全屏展示
- 功能2：简易对话功能，支持文字与图表回答（基于 Chart.js）
- 功能3：另一组下拉框选择并加载 HTML 文件，全屏展示

### 运行方式

方式一（推荐）：使用 Node 服务
1. 安装依赖：`npm install`
2. 启动服务：`npm start`
3. 访问：`http://localhost:5173`

方式二：使用 Python 简易服务
1. 进入 `public` 目录：`cd public`
2. 启动服务：`python3 -m http.server 5173`
3. 访问：`http://localhost:5173`

注意：直接双击打开 `index.html`（file:// 协议）会因为浏览器安全策略导致局部 HTML 加载失败，建议使用以上任一方式启动本地服务。

### 目录结构

```
public/
  content/
    exhibit1.html
    exhibit2.html
    showcase1.html
    showcase2.html
  images/
    cover*.svg / figure*.svg / complex-diagram.svg
  index.html
  styles.css
  app.js
package.json
server.js
```

### 自定义
- 可在 `public/content/` 下添加更多 HTML 文件，并在页面下拉框中新增对应选项。
- 对话功能的示例图表逻辑位于 `public/app.js` 中的 `generateAnswer` 与 `renderChartInMessage`。

### 预设问题与埋点答案（功能2）
- 预设问题按钮定义在 `public/index.html` 的 `chat-presets` 区域：
  - 按钮示例：`<button class="preset" data-q="查看最近7天访问量（柱状图）">7天访问量（柱状）</button>`
  - 点击后会读取 `data-q` 文本并直接发送提问。
- 预设问题与固定答案在 `public/app.js` 的 `presetAnswers` 中配置，支持以下类型：
```js
const presetAnswers = {
  // 图表答案（保留原有示例）
  '查看最近7天访问量（柱状图）': { text: '这是最近7天访问量的柱状图：', type: 'bar', aliases: ['7天 访问量 柱状', '七天 柱状 图'] },
  '查看最近7天访问量（折线图）': { text: '这是最近7天访问量的折线图：', type: 'line', aliases: ['7天 访问量 折线', '七天 折线 图'] },

  // 文字 + 表格答案
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

  // 图片 + 文字答案
  '查看产品结构（图片）': {
    text: '这是产品结构图（示意）：',
    aliases: ['产品 结构', '产品 图片', '结构 图片'],
    image: { src: 'images/complex-diagram.svg', alt: '产品结构示意', caption: '系统架构与数据流总览' }
  }
};
```
- 扩展方式：
  - 新增预设按钮：在 `index.html` 增加一个 `data-q="你的问题"` 的按钮。
  - 新增答案：在 `app.js` 的 `presetAnswers` 中用问题全文作为 key，写入以下结构之一：
    - 图表：`{ text, type: 'bar' | 'line' }`
    - 表格：`{ text, table: { headers: string[], rows: string[][] } }`
    - 图片：`{ text, image: { src, alt?, caption? } }`
  - 可增加 `aliases`（字符串数组）用于模糊匹配同义表达。
  - 当用户问题包含预设问题全文或别名之一时，将直接返回对应预设答案（包含图表/表格/图片），不会回显“已收到：xxx”。

# Luojuanyun