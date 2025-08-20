# DOCX 转 HTML（嵌入 Base64 图片）

- 支持图片、表格
- UTF-8 防乱码（需系统有中文字体）
- 保留原样式（依赖 XWPF -> XHTML 转换）
- 图片以 Base64 嵌入 HTML

## 构建

请使用本地已安装的 Gradle 或 IDE 构建。

```
# 无 wrapper 场景（示例）：
# 在本仓库目录执行（需已安装 Gradle）
# gradle clean shadowJar
```

若无 Gradle 环境，可在 IDE 里导入 Gradle 工程并执行 `shadowJar` 任务。

构建产物：`build/libs/docx2html-1.0.0-all.jar`

## 运行

```
java -jar build/libs/docx2html-1.0.0-all.jar input.docx output.html
```

- `input.docx`：源 Word 文档
- `output.html`：输出 HTML 文件

## 备注

- 若出现中文字体显示异常，请确保运行环境安装了中文字体（如 `Noto Sans CJK`）。
- 对非常复杂的 Word 样式，HTML 表达可能存在差异，建议结合目标场景测试与微调 CSS。
