// UI check of the plugin's webapp addons, run by scripts/integration-test.sh.
//
// Opens the pages the addons render on a project bound to an ALM with a branch
// and a pull request, and fails on:
//   * JavaScript errors on the page,
//   * failed (>= 400) requests to SonarQube itself (e.g. missing images),
//   * utility classes written in a different prefix syntax than the webapp's
//     own CSS uses (e.g. Tailwind 3 'sw-flex' against Tailwind 4 'sw:flex'),
//     which leaves whole components unstyled,
//   * missing expected content (case-insensitive, as wording varies per version).
// Individual utility classes in the webapp's own syntax that have no CSS rule are
// reported as warnings: older webapps never generated some addon-only classes.
// Screenshots of every page are written to $UI_OUT for the CI artifacts.
//
// Environment: SONARQUBE_URL, UI_LOGIN, UI_PASSWORD, UI_PROJECT, UI_OUT.
const { chromium } = require('playwright');

const BASE = process.env.SONARQUBE_URL || 'http://localhost:9000';
const PROJECT = process.env.UI_PROJECT;
const OUT = process.env.UI_OUT || 'build/integration-test/ui';

const PAGES = [
  { name: 'branches', path: `/project/branches?id=${PROJECT}`, expect: ['Branches & pull requests', 'main'] },
  {
    name: 'pull-requests', path: `/project/branches?id=${PROJECT}`, expect: ['feature/x'],
    action: async (page) => page.getByRole('tab', { name: /Pull Requests/i })
      .or(page.getByText('Pull Requests', { exact: true })).first().click({ timeout: 10000 }),
  },
  { name: 'pull-request-overview', path: `/dashboard?id=${PROJECT}&pullRequest=1`, expect: ['Failed', 'feature/x', 'New issues'] },
  { name: 'pull-request-issues', path: `/project/issues?id=${PROJECT}&pullRequest=1&issueStatuses=OPEN`, expect: ['app.py'] },
  {
    name: 'branch-selector', path: `/dashboard?id=${PROJECT}`, expect: ['main', 'feature/x'],
    action: async (page) => page.getByRole('button', { name: /main/i }).first().click({ timeout: 10000 }),
  },
];

// Collect every utility class used on the page that has no CSS rule, and the prefix
// syntax ('sw:' for Tailwind 4, 'sw-' for Tailwind 3) the webapp's own CSS uses.
function undefinedUtilityClasses() {
  const used = new Set();
  for (const element of document.querySelectorAll('[class]')) {
    for (const token of String(element.className.baseVal ?? element.className).split(/\s+/)) {
      if (/^-?sw[-:]/.test(token)) used.add(token);
    }
  }
  const defined = new Set();
  const collect = (rules) => {
    for (const rule of rules) {
      if (rule.selectorText) {
        for (const match of rule.selectorText.matchAll(/\.((?:\\.|[\w-])+)/g)) {
          defined.add(match[1].replace(/\\(.)/g, '$1'));
        }
      }
      if (rule.cssRules) collect(rule.cssRules);
    }
  };
  for (const sheet of document.styleSheets) {
    try { collect(sheet.cssRules); } catch (e) { /* cross-origin sheet */ }
  }
  const webappSyntax = [...defined].some((name) => /^-?sw:/.test(name)) ? 'sw:' : 'sw-';
  const missing = [...used].filter((token) => !defined.has(token)).sort();
  return { webappSyntax, missing };
}

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  const origin = new URL(BASE).origin;
  const failures = [];
  const warnings = [];
  let current = 'login';
  page.on('pageerror', (error) => failures.push(`${current}: JavaScript error: ${String(error).split('\n')[0]}`));
  page.on('response', (response) => {
    if (response.status() >= 400 && response.url().startsWith(origin)) {
      failures.push(`${current}: HTTP ${response.status()} for ${response.url().replace(origin, '')}`);
    }
  });

  const login = await context.request.post(`${BASE}/api/authentication/login`, {
    form: { login: process.env.UI_LOGIN || 'admin', password: process.env.UI_PASSWORD },
  });
  if (!login.ok()) throw new Error(`login failed with HTTP ${login.status()}`);

  for (const spec of PAGES) {
    current = spec.name;
    await page.goto(BASE + spec.path, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(1500);
    for (const label of ['Dismiss', 'Got it']) {
      for (const button of await page.getByRole('button', { name: label, exact: true }).all()) {
        await button.click({ timeout: 2000 }).catch(() => {});
      }
    }
    if (spec.action) {
      try { await spec.action(page); await page.waitForTimeout(1500); } catch (e) {
        failures.push(`${current}: could not interact with the page: ${String(e).split('\n')[0]}`);
      }
    }
    const text = (await page.locator('body').innerText()).toLowerCase();
    for (const expected of spec.expect) {
      if (!text.includes(expected.toLowerCase())) failures.push(`${current}: expected text "${expected}" not found`);
    }
    const { webappSyntax, missing } = await page.evaluate(undefinedUtilityClasses);
    const otherSyntax = missing.filter((token) => !token.replace(/^-/, '').startsWith(webappSyntax));
    const sameSyntax = missing.filter((token) => !otherSyntax.includes(token));
    if (otherSyntax.length) {
      failures.push(`${current}: ${otherSyntax.length} utility classes use a different prefix syntax than the webapp's '${webappSyntax}' and have no CSS: ${otherSyntax.join(' ')}`);
    }
    if (sameSyntax.length) warnings.push(`${current}: ${sameSyntax.length} utility classes without CSS: ${sameSyntax.join(' ')}`);
    await page.screenshot({ path: `${OUT}/${spec.name}.png` });
    console.log(`UI ${spec.name}: checked`);
  }
  await browser.close();

  for (const warning of warnings) console.log(`UI warning: ${warning}`);

  if (failures.length) {
    console.error('UI check: FAILED');
    for (const failure of failures) console.error(`  - ${failure}`);
    process.exit(1);
  }
  console.log(`UI check: OK (${PAGES.length} pages, screenshots in ${OUT})`);
})().catch((error) => { console.error('UI check: FAILED', error); process.exit(1); });
