#!/usr/bin/env python3
"""Assert what the plugin sent to the ALM mock for one pull request analysis.

Usage: assert-decoration.py <alm> <requests.jsonl> <issues.json> <qualitygate.json>

Expected values come from SonarQube itself: the lines of the open issues on the
pull request and its quality gate status. The fixture adds issues, so the gate is
expected to fail; the test checks the plugin reports exactly those issue lines
inline, posts a summary, and marks the pull request as failed.
"""
import json
import sys
from urllib.parse import parse_qs

alm, log_path, issues_path, gate_path = sys.argv[1:5]
requests = [json.loads(line) for line in open(log_path) if line.strip()]
issues = json.load(open(issues_path))["issues"]
gate = json.load(open(gate_path))["projectStatus"]["status"]
expected_lines = sorted({i["line"] for i in issues if "line" in i})
failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def calls(method, path_suffix):
    return [r for r in requests if r["method"] == method and r["path"].endswith(path_suffix)]


def json_body(request):
    try:
        return json.loads(request["body"])
    except ValueError:
        return None


check(len(expected_lines) > 0, "SonarQube reported no issues with a line on the pull request; the fixture should add some")
check(gate == "ERROR", f"expected the pull request quality gate to fail, got {gate}")
check(all(r["auth"] for r in requests), "some requests to the ALM were sent without credentials")
unmatched = sorted({f'{r["method"]} {r["path"]}' for r in requests if not r["matched"]})
check(not unmatched, f"the plugin called endpoints the mock does not know: {unmatched}")

if alm == "gitlab":
    posts = [parse_qs(r["body"]) for r in calls("POST", "/merge_requests/1/discussions")]
    inline = sorted(int(p["position[new_line]"][0]) for p in posts if "position[new_line]" in p)
    summaries = [p for p in posts if "position[new_line]" not in p]
    statuses = [r for r in requests if r["method"] == "POST" and "/statuses/" in r["path"]]
    check(inline == expected_lines, f"inline discussions on lines {inline}, expected {expected_lines}")
    check(len(summaries) == 1, f"expected 1 summary discussion, got {len(summaries)}")
    check(len(statuses) == 1 and "state=failed" in statuses[0]["query"], f"expected one failed commit status, got {[s['query'] for s in statuses]}")
elif alm == "github":
    runs = [json_body(r) for r in calls("POST", "/check-runs")]
    comments = calls("POST", "/issues/1/comments")
    check(len(runs) == 1, f"expected 1 check run, got {len(runs)}")
    if runs:
        run = runs[0]
        lines = sorted(a["start_line"] for a in run.get("output", {}).get("annotations", []))
        check(run.get("conclusion") == "failure", f"check run conclusion {run.get('conclusion')}, expected failure")
        check(lines == expected_lines, f"check run annotations on lines {lines}, expected {expected_lines}")
    check(len(comments) == 1, f"expected 1 summary comment, got {len(comments)}")
elif alm == "azure":
    threads = [json_body(r) for r in calls("POST", "/pullRequests/1/threads")]
    inline = sorted(t["threadContext"]["rightFileStart"]["line"] for t in threads if t and t.get("threadContext"))
    summaries = [t for t in threads if t and not t.get("threadContext")]
    statuses = [json_body(r) for r in calls("POST", "/pullRequests/1/statuses")]
    check(inline == expected_lines, f"inline threads on lines {inline}, expected {expected_lines}")
    check(len(summaries) == 1, f"expected 1 summary thread, got {len(summaries)}")
    check(len(statuses) == 1 and statuses[0].get("state") == "ERROR", f"expected one ERROR status, got {statuses}")
elif alm == "bitbucket":
    reports = [json_body(r) for r in calls("PUT", "/reports/com.sonarsource.sonarqube")]
    annotations = [a for r in calls("POST", "/reports/com.sonarsource.sonarqube/annotations")
                   for a in (json_body(r) or {}).get("annotations", [])]
    lines = sorted(a["line"] for a in annotations)
    check(len(reports) == 1 and reports[0].get("result") == "FAIL", f"expected one FAIL report, got {reports}")
    check(lines == expected_lines, f"annotations on lines {lines}, expected {expected_lines}")
elif alm == "bitbucketcloud":
    tokens = [r for r in requests if r["host"] == "bitbucket.org" and r["path"].endswith("/site/oauth2/access_token")]
    reports = [json_body(r) for r in calls("PUT", "/reports/com.sonarsource.sonarqube")]
    annotations = [a for r in calls("POST", "/reports/com.sonarsource.sonarqube/annotations") for a in (json_body(r) or [])]
    lines = sorted(a["line"] for a in annotations)
    builds = [json_body(r) for r in calls("POST", "/statuses/build")]
    check(len(tokens) >= 1, "no OAuth token was requested from bitbucket.org")
    check(len(reports) == 1 and reports[0].get("result") == "FAILED", f"expected one FAILED report, got {reports}")
    check(lines == expected_lines, f"annotations on lines {lines}, expected {expected_lines}")
    check(len(builds) == 1 and builds[0].get("state") == "FAILED", f"expected one FAILED build status, got {builds}")
else:
    failures.append(f"unknown ALM {alm}")

if failures:
    print(f"ALM decoration {alm}: FAILED", file=sys.stderr)
    for failure in failures:
        print(f"  - {failure}", file=sys.stderr)
    sys.exit(1)
print(f"ALM decoration {alm}: OK ({len(requests)} calls, issues inline on lines {expected_lines}, failed status reported)")
