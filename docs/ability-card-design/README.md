# FiveLines 54 张能力卡设计图

- `cards/FL-01.png` 至 `cards/FL-54.png`：54 张独立卡面，568×796。
- `sheets/ability-cards-all-54.png`：完整卡册长图。
- `sheets/01-line.png` 至 `sheets/07-theme.png`：7 个分组总览。
- `index.html`：可直接浏览的完整卡册。

卡面采用统一的 FiveLines 街机棋盘语言，七个能力分组各有独立强调色。卡号、名称、点亮条件、卡片能力和收藏/装备类型均与当前产品目录一致。

重新生成需要 Playwright。推荐将依赖安装在临时目录后，通过 `PLAYWRIGHT_MODULE` 指向模块入口，避免在项目内产生 `node_modules`。

```powershell
$renderTools = Join-Path $env:TEMP 'fivelines-card-render'
npm install --prefix $renderTools playwright@1.63.0 --no-save
$env:PLAYWRIGHT_MODULE = Join-Path $renderTools 'node_modules\playwright'
node .\docs\ability-card-design\render.mjs
```
