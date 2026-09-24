import { mkdir } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const root = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.join(root, 'concepts');
await mkdir(outDir, { recursive: true });
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const browser = await chromium.launch({ headless: true, channel: 'msedge' });
const url = pathToFileURL(path.join(root, 'concept-samples.html')).href;
const ids = [
  'sample-01-arcade-chip', 'sample-02-tactics-board', 'sample-03-medal-collection',
  'sample-04-classic-rally', 'sample-05-retro-sports', 'sample-06-pixel-city-car', 'sample-07-classic-microcar'
];
for (const id of ids) {
  const page = await browser.newPage({ viewport: { width: 680, height: 920 }, deviceScaleFactor: 1 });
  await page.goto(`${url}?card=${id}`, { waitUntil: 'load' });
  await page.locator('.ability-card.capture').screenshot({ path: path.join(outDir, `${id}.png`) });
  await page.close();
}
const overview = await browser.newPage({ viewport: { width: 2480, height: 1800 }, deviceScaleFactor: 1 });
await overview.goto(url, { waitUntil: 'load' });
await overview.locator('#catalog').screenshot({ path: path.join(outDir, 'concept-overview.png') });
await overview.close();
await browser.close();
console.log(`Rendered ${ids.length} concept cards and overview to ${outDir}`);
