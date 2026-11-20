#!/usr/bin/env python3
"""Summarise an integration test run for CI.

Reads the results scripts/integration-test.sh leaves in build/integration-test:
  analysis.json           branch and pull request analysis checks
  alm/result-<alm>.json   ALM decoration checks (assert-decoration.py)
  ui/results.json + PNGs  UI check (ui-test.cjs)
and writes, in that directory:
  summary.md                     Markdown tables for the job summary
  integration-test-report.html   the same tables with every screenshot embedded,
                                 a single self-contained file (no external CSS/JS)
                                 that GitHub can show in the browser when it is
                                 uploaded as an unzipped artifact.

Enabled checks without a result (because an earlier step failed) are reported as
incomplete and count against the headline like failures; checks of a suite that
was switched off with INTEGRATION_ALM/INTEGRATION_UI are reported as not run.
Always exits 0; the test script's own exit code decides whether the job fails.
"""
import base64
import html
import json
import os
import sys

RESULTS = sys.argv[1] if len(sys.argv) > 1 else "build/integration-test"
# The directory is missing when the job failed before the test script ran.
os.makedirs(RESULTS, exist_ok=True)
ALMS = os.environ.get("ALM_TEST_ALMS", "gitlab github azure bitbucket bitbucketcloud").split()
ALM_NAMES = {"gitlab": "GitLab", "github": "GitHub", "azure": "Azure DevOps",
             "bitbucket": "Bitbucket Server", "bitbucketcloud": "Bitbucket Cloud"}
UI_PAGES = ["branches", "pull-requests", "pull-request-overview", "pull-request-issues", "branch-selector"]
ICON = {"passed": "✅", "warning": "⚠️", "failed": "❌", "incomplete": "❓", "not run": "⏭️"}


def load(path):
    try:
        with open(os.path.join(RESULTS, path)) as f:
            return json.load(f)
    except (OSError, ValueError):
        return None


def enabled(variable):
    # Same rule as scripts/integration-test.sh: a suite runs only when its toggle
    # is unset or exactly "true".
    return os.environ.get(variable, "true") == "true"


def toggle(variable):
    return f"{variable}={os.environ.get(variable, '')}"


rows = []  # (section, check, status, details)

analysis = load("analysis.json")
rows.append(("Analyses", "Main branch, branch and pull request analysis",
             "passed" if analysis and analysis.get("ok") else "incomplete",
             "; ".join(analysis.get("checks", [])) if analysis else "no result; see the test log"))

for alm in ALMS:
    name = ALM_NAMES.get(alm, alm)
    if not enabled("INTEGRATION_ALM"):
        rows.append(("ALM decoration", name, "not run", toggle("INTEGRATION_ALM")))
        continue
    result = load(f"alm/result-{alm}.json")
    if result is None:
        rows.append(("ALM decoration", name, "incomplete", "no result (an earlier step failed)"))
    elif result["ok"]:
        rows.append(("ALM decoration", name, "passed",
                     f"{result['calls']} calls; issues inline on lines {result['issue_lines']}; failed status reported"))
    else:
        rows.append(("ALM decoration", name, "failed", "; ".join(result["failures"])))

ui = load("ui/results.json") if enabled("INTEGRATION_UI") else None
for page in UI_PAGES + sorted(k for k in (ui or {}) if k not in UI_PAGES):
    if not enabled("INTEGRATION_UI"):
        rows.append(("UI", page, "not run", toggle("INTEGRATION_UI")))
        continue
    result = (ui or {}).get(page)
    if result is None:
        rows.append(("UI", page, "incomplete", "no result (an earlier step failed)"))
    elif result["failures"]:
        rows.append(("UI", page, "failed", "; ".join(result["failures"] + result.get("warnings", []))))
    elif result.get("warnings"):
        rows.append(("UI", page, "warning", "; ".join(result["warnings"])))
    else:
        rows.append(("UI", page, "passed", ""))

failed = sum(1 for r in rows if r[2] == "failed")
incomplete = sum(1 for r in rows if r[2] == "incomplete")
ran = sum(1 for r in rows if r[2] != "not run")
if failed or incomplete:
    problems = [f"{failed} failed"] if failed else []
    problems += [f"{incomplete} incomplete"] if incomplete else []
    headline = f"❌ {' and '.join(problems)} of {ran} checks"
else:
    headline = f"✅ All {ran} checks passed"


def markdown_cell(text):
    return text.replace("|", "\\|").replace("\n", " ")


summary = [f"## Integration test: {headline}", ""]
for section in dict.fromkeys(r[0] for r in rows):
    summary += [f"### {section}", "", "| | Check | Details |", "|---|---|---|"]
    summary += [f"| {ICON[status]} | {check} | {markdown_cell(details)} |"
                for sec, check, status, details in rows if sec == section]
    summary.append("")
with open(os.path.join(RESULTS, "summary.md"), "w") as f:
    f.write("\n".join(summary) + "\n")

parts = [
    "<!doctype html><html lang='en'><head><meta charset='utf-8'>",
    "<meta name='viewport' content='width=device-width, initial-scale=1'>",
    "<title>Integration test report</title><style>",
    "body{font-family:system-ui,sans-serif;margin:24px;color:#1f2328;background:#fff}",
    "table{border-collapse:collapse;margin-bottom:24px;width:100%}",
    "th,td{border:1px solid #d0d7de;padding:6px 10px;text-align:left;vertical-align:top}",
    "th{background:#f6f8fa}td.s{width:2em;text-align:center}",
    "figure{margin:0 0 32px}img{max-width:100%;border:1px solid #d0d7de}",
    "figcaption{font-weight:600;margin-bottom:8px}",
    "</style></head><body>",
    f"<h1>Integration test: {html.escape(headline)}</h1>",
]
for section in dict.fromkeys(r[0] for r in rows):
    parts.append(f"<h2>{html.escape(section)}</h2><table><tr><th></th><th>Check</th><th>Details</th></tr>")
    parts += [f"<tr><td class='s'>{ICON[status]}</td><td>{html.escape(check)}</td><td>{html.escape(details)}</td></tr>"
              for sec, check, status, details in rows if sec == section]
    parts.append("</table>")
screenshots = [(page, os.path.join(RESULTS, "ui", (ui or {}).get(page, {}).get("screenshot", f"{page}.png")))
               for page in UI_PAGES]
screenshots = [(page, path) for page, path in screenshots if os.path.exists(path)]
if screenshots:
    parts.append("<h2>Screenshots</h2>")
    for page, path in screenshots:
        with open(path, "rb") as f:
            data = base64.b64encode(f.read()).decode()
        parts.append(f"<figure><figcaption>{html.escape(page)}</figcaption>"
                     f"<img alt='{html.escape(page)}' src='data:image/png;base64,{data}'></figure>")
parts.append("</body></html>")
with open(os.path.join(RESULTS, "integration-test-report.html"), "w") as f:
    f.write("\n".join(parts))

print("\n".join(summary))
