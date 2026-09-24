import { createRequire } from 'node:module';
import { mkdir } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';

const root = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const cardsDir = path.join(root, 'cards');
const sheetsDir = path.join(root, 'sheets');
await mkdir(cardsDir, { recursive: true });
await mkdir(sheetsDir, { recursive: true });

const browser = await chromium.launch({ headless: true, channel: 'msedge' });
const page = await browser.newPage({ viewport: { width: 2600, height: 1800 }, deviceScaleFactor: 1 });
const url = pathToFileURL(path.join(root, 'index.html')).href;
await page.goto(url, { waitUntil: 'load' });
await page.locator('#catalog').screenshot({ path: path.join(sheetsDir, 'ability-cards-all-54.png') });

const groups = await page.locator('.group').count();
for (let i = 0; i < groups; i += 1) {
  const group = page.locator('.group').nth(i);
  const key = await group.getAttribute('data-group');
  await group.screenshot({ path: path.join(sheetsDir, `${String(i + 1).padStart(2, '0')}-${key}.png`) });
}

for (let i = 1; i <= 54; i += 1) {
  const number = String(i).padStart(2, '0');
  const cardPage = await browser.newPage({ viewport: { width: 680, height: 920 }, deviceScaleFactor: 1 });
  await cardPage.goto(`${url}?card=${number}`, { waitUntil: 'load' });
  await cardPage.locator('.ability-card.capture').screenshot({ path: path.join(cardsDir, `FL-${number}.png`) });
  await cardPage.close();
}
await browser.close();
console.log(`Rendered 54 cards and ${groups + 1} catalog sheets to ${root}`);
