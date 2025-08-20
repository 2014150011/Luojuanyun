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
  index.html
  styles.css
  app.js
package.json
server.js
```

### 自定义
- 可在 `public/content/` 下添加更多 HTML 文件，并在页面下拉框中新增对应选项。
- 对话功能的示例图表逻辑位于 `public/app.js` 中的 `generateAnswer` 与 `renderChartInMessage`。

# Luojuanyun