// UI check of the plugin's webapp addons, run by scripts/integration-test.sh.
//
// Opens the pages the addons render on a project bound to an ALM with a branch
// and a pull request, and fails on:
//   * JavaScript errors on the page,
//   * failed requests to SonarQube itself: any >= 400 for static resources (e.g.
//     missing images) and >= 500 for the Web API (an API 4xx is often a normal
//     answer, such as 404 from api/alm_settings/get_binding for an unbound project),
//   * utility classes written in a different prefix syntax than the webapp's
//     own CSS uses (e.g. Tailwind 3 'sw-flex' against Tailwind 4 'sw:flex'),
//     which leaves whole components unstyled,
//   * missing expected content (case-insensitive, as wording varies per version).
// Individual utility classes in the webapp's own syntax that have no CSS rule are
// reported as warnings: older webapps never generated some addon-only classes.
// Screenshots of every page and a results.json with the outcome per page are
// written to $UI_OUT, for the CI artifacts and report (scripts/integration-test/report.py).
//
// Environment: SONARQUBE_URL, UI_LOGIN, UI_PASSWORD, UI_PROJECT, UI_OUT.
const fs = require('fs');
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

// Shared by the run and the error handler, so an unexpected error keeps what was collected.
const failures = [];
const warnings = [];
const results = {};
let current = 'login';

function record(kind, message) {
  results[current] = results[current] || { failures: [], warnings: [] };
  results[current][kind].push(message);
  (kind === 'failures' ? failures : warnings).push(`${current}: ${message}`);
}
const fail = (message) => record('failures', message);
const warn = (message) => record('warnings', message);
const firstLine = (error) => String(error).split('\n')[0];

async function checkPage(page, spec) {
  await page.goto(BASE + spec.path, { waitUntil: 'networkidle', timeout: 60000 });
  await page.waitForTimeout(1500);
  for (const label of ['Dismiss', 'Got it']) {
    for (const button of await page.getByRole('button', { name: label, exact: true }).all()) {
      await button.click({ timeout: 2000 }).catch(() => {});
    }
  }
  if (spec.action) {
    try { await spec.action(page); await page.waitForTimeout(1500); } catch (e) {
      fail(`could not interact with the page: ${firstLine(e)}`);
    }
  }
  const text = (await page.locator('body').innerText()).toLowerCase();
  for (const expected of spec.expect) {
    if (!text.includes(expected.toLowerCase())) fail(`expected text "${expected}" not found`);
  }
  const { webappSyntax, missing } = await page.evaluate(undefinedUtilityClasses);
  const otherSyntax = missing.filter((token) => !token.replace(/^-/, '').startsWith(webappSyntax));
  const sameSyntax = missing.filter((token) => !otherSyntax.includes(token));
  if (otherSyntax.length) {
    fail(`${otherSyntax.length} utility classes use a different prefix syntax than the webapp's '${webappSyntax}' and have no CSS: ${otherSyntax.join(' ')}`);
  }
  if (sameSyntax.length) warn(`${sameSyntax.length} utility classes without CSS: ${sameSyntax.join(' ')}`);
}

(async () => {
  const browser = await chromium.launch();
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();
    const origin = new URL(BASE).origin;
    page.on('pageerror', (error) => fail(`JavaScript error: ${firstLine(error)}`));
    page.on('response', (response) => {
      if (!response.url().startsWith(origin)) return;
      const path = response.url().replace(origin, '');
      const isApi = path.startsWith('/api/');
      if (response.status() >= 500 || (!isApi && response.status() >= 400)) {
        fail(`HTTP ${response.status()} for ${path}`);
      }
    });

    const login = await context.request.post(`${BASE}/api/authentication/login`, {
      form: { login: process.env.UI_LOGIN || 'admin', password: process.env.UI_PASSWORD },
    });
    if (!login.ok()) throw new Error(`login failed with HTTP ${login.status()}`);

    for (const spec of PAGES) {
      current = spec.name;
      results[current] = { failures: [], warnings: [], screenshot: `${spec.name}.png` };
      // A page that fails to load or crashes is recorded as that page's failure; the others still run.
      try {
        await checkPage(page, spec);
      } catch (e) {
        fail(`page check aborted: ${firstLine(e)}`);
      }
      await page.screenshot({ path: `${OUT}/${spec.name}.png` }).catch(() => {
        delete results[current].screenshot;
      });
      console.log(`UI ${spec.name}: checked`);
    }
  } finally {
    await browser.close().catch(() => {});
  }

  for (const warning of warnings) console.log(`UI warning: ${warning}`);
  writeResults(results);

  if (failures.length) {
    console.error('UI check: FAILED');
    for (const failure of failures) console.error(`  - ${failure}`);
    process.exit(1);
  }
  console.log(`UI check: OK (${PAGES.length} pages, screenshots in ${OUT})`);
})().catch((error) => {
  // Keep the results of the pages that were already checked.
  fail(`run aborted: ${firstLine(error)}`);
  for (const failure of failures) console.error(`  - ${failure}`);
  console.error('UI check: FAILED');
  writeResults(results);
  process.exit(1);
});

function writeResults(collected) {
  fs.writeFileSync(`${OUT}/results.json`, JSON.stringify(collected, null, 2));
}
