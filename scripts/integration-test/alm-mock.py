#!/usr/bin/env python3
"""Recording mock of the ALM APIs the plugin decorates pull requests on.

Runs as the `alm-mock` compose service (see compose.alm-mock.yml) and serves:
  * plain HTTP on port 8080 for ALMs whose URL is configurable:
      GitLab           /api/v4/...
      Azure DevOps     /azure/...
      Bitbucket Server /bbs/...
      GitHub           /gh/...
  * HTTPS on port 443 for Bitbucket Cloud, whose hosts (bitbucket.org and
    api.bitbucket.org) are hard-coded in the plugin. The service carries those
    names as network aliases, and SonarQube trusts the test CA that signed the
    certificate.

Every request is appended as one JSON line to $LOG, so the test can assert what
the plugin sent. Responses are minimal payloads shaped like the real APIs (and
like the plugin's own WireMock integration tests). Unknown routes answer {} so
any gap shows up as an error in SonarQube's Compute Engine log.

Environment: HEAD_SHA / BASE_SHA (fixture commits), LOG, TLS_CERT, TLS_KEY.
"""
import json
import os
import re
import ssl
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit

LOG = os.environ.get("LOG", "/data/requests.jsonl")
SHA = os.environ.get("HEAD_SHA", "0" * 40)
BASE = os.environ.get("BASE_SHA", "0" * 40)
LOCK = threading.Lock()


def gitlab(m, p, q, body):
    if p == "/api/v4/user":
        return 200, {"id": 1, "username": "sonarqube-bot"}
    mr = re.fullmatch(r"/api/v4/projects/([^/]+)/merge_requests/(\d+)", p)
    if mr and m == "GET":
        return 200, {"id": 15235, "iid": int(mr.group(2)), "source_project_id": 42, "target_project_id": 42,
                     "web_url": "http://gitlab.mock/group/project/-/merge_requests/1",
                     "diff_refs": {"base_sha": BASE, "start_sha": BASE, "head_sha": SHA}}
    if re.fullmatch(r"/api/v4/projects/\d+/merge_requests/\d+/commits", p):
        return 200, [{"id": SHA}]
    if re.fullmatch(r"/api/v4/projects/\d+/merge_requests/\d+/discussions", p):
        if m == "GET":
            return 200, []
        return 201, {"id": "d1", "individual_note": False, "notes": [
            {"id": 1, "type": "DiscussionNote", "body": "x", "author": {"id": 1, "username": "sonarqube-bot"},
             "resolved": False, "system": False, "resolvable": True}]}
    if re.fullmatch(r"/api/v4/projects/\d+/merge_requests/\d+/discussions/[^/]+(/notes(/\d+)?)?", p):
        return 200, {"id": "d1"}
    if re.fullmatch(r"/api/v4/projects/\d+/statuses/[0-9a-f]+", p):
        return 201, {"id": 1, "status": q.get("state", [""])[0]}
    return None


def azure(m, p, q, body):
    if not p.startswith("/azure/"):
        return None
    p = p[len("/azure"):]
    if p == "/_apis/ConnectionData":
        return 200, {"authenticatedUser": {"id": "u1", "providerDisplayName": "SonarQube Bot",
                                           "customDisplayName": "SonarQube Bot"}}
    pr = re.fullmatch(r"/([^/]+)/_apis/git/repositories/([^/]+)/pullRequests/(\d+)", p)
    if pr:
        proj, repo, n = pr.groups()
        return 200, {"repository": {"id": "r1", "name": repo, "project": {"id": "p1", "name": proj},
                                    "remoteUrl": "http://azure.mock/" + proj + "/_git/" + repo},
                     "pullRequestId": int(n), "status": "active", "sourceRefName": "refs/heads/feature/x",
                     "targetRefName": "refs/heads/main", "lastMergeSourceCommit": {"commitId": SHA},
                     "lastMergeTargetCommit": {"commitId": BASE}}
    if re.search(r"/pullRequests/\d+/iterations$", p):
        return 200, {"value": [{"id": 1}], "count": 1}
    if re.search(r"/pullRequests/\d+/commits$", p):
        return 200, {"value": [{"commitId": SHA}], "count": 1}
    if re.search(r"/pullRequests/\d+/threads$", p):
        if m == "GET":
            return 200, {"value": [], "count": 0}
        return 200, {"id": 101, "status": "active", "isDeleted": False,
                     "comments": [{"id": 1, "content": "x", "isDeleted": False, "commentType": "text",
                                   "author": {"id": "u1"}}]}
    if re.search(r"/pullRequests/\d+/threads/\d+(/comments)?$", p):
        return 200, {"id": 1, "isDeleted": False}
    if re.search(r"/pullRequests/\d+/statuses$", p):
        return 200, {"id": 1}
    return None


def bitbucket_server(m, p, q, body):
    if not p.startswith("/bbs/"):
        return None
    p = p[len("/bbs"):]
    if p == "/rest/api/1.0/application-properties":
        return 200, {"version": "8.19.0", "buildNumber": "8019000", "displayName": "Bitbucket"}
    rp = re.fullmatch(r"/rest/api/1.0/projects/([^/]+)/repos/([^/]+)", p)
    if rp:
        return 200, {"slug": rp.group(2), "id": 1, "name": rp.group(2),
                     "project": {"key": rp.group(1), "id": 1, "name": rp.group(1)}}
    if re.search(r"/commits/[0-9a-f]+/builds$", p):
        return 204, None
    if re.search(r"/rest/insights/1.0/.*/reports/[^/]+/annotations$", p):
        return 204, None
    if re.search(r"/rest/insights/1.0/.*/reports/[^/]+$", p):
        return 200, {"key": "x"}
    return None


def github(m, p, q, body):
    if not p.startswith("/gh/"):
        return None
    p = p[len("/gh"):]
    api = "http://GHHOST/gh"
    if p == "/app":
        return 200, {"id": 12345, "slug": "sonarqube-mock", "name": "SonarQube Mock", "owner": {"login": "acme", "id": 1}}
    rm = re.fullmatch(r"/repos/([^/]+)/([^/]+)/installation", p)
    if rm:
        return 200, {"id": 777, "app_id": 12345, "account": {"login": rm.group(1), "id": 1},
                     "access_tokens_url": api + "/app/installations/777/access_tokens"}
    if re.fullmatch(r"/app/installations/\d+/access_tokens", p):
        return 201, {"token": "ghs_mocktoken", "expires_at": "2099-01-01T00:00:00Z", "permissions": {},
                     "repository_selection": "all"}
    rr = re.fullmatch(r"/repos/([^/]+)/([^/]+)", p)
    if rr:
        o, r = rr.groups()
        return 200, {"id": 1, "name": r, "full_name": o + "/" + r, "owner": {"login": o, "id": 1},
                     "url": api + "/repos/" + o + "/" + r, "html_url": "http://github.mock/" + o + "/" + r}
    if re.fullmatch(r"/repos/[^/]+/[^/]+/check-runs", p):
        return 201, {"id": 555, "name": "SonarQube", "status": "completed", "head_sha": SHA}
    pl = re.fullmatch(r"/repos/([^/]+)/([^/]+)/pulls/(\d+)", p)
    if pl:
        o, r, n = pl.groups()
        base = api + "/repos/" + o + "/" + r
        return 200, {"id": 9, "number": int(n), "state": "open",
                     "html_url": "http://github.mock/" + o + "/" + r + "/pull/" + n,
                     "url": base + "/pulls/" + n, "issue_url": base + "/issues/" + n,
                     "comments_url": base + "/issues/" + n + "/comments",
                     "head": {"sha": SHA, "ref": "feature/x"}, "base": {"sha": BASE, "ref": "main"},
                     "user": {"login": "dev"}}
    if re.fullmatch(r"/repos/[^/]+/[^/]+/issues/\d+/comments", p):
        if m == "GET":
            return 200, []
        return 201, {"id": 4242, "body": "x", "user": {"login": "sonarqube-mock[bot]", "type": "Bot"}}
    if p == "/graphql":
        return 200, {"data": {}}
    return None


def bitbucket_cloud(m, p, q, body):
    if p == "/bbc-auth/site/oauth2/access_token":
        return 200, {"access_token": "bbc-mock-token", "token_type": "bearer", "expires_in": 7200,
                     "scopes": "repository pullrequest"}
    if not p.startswith("/bbc/"):
        return None
    p = p[len("/bbc"):]
    rp = re.fullmatch(r"/2.0/repositories/([^/]+)/([^/]+)", p)
    if rp:
        return 200, {"type": "repository", "full_name": rp.group(1) + "/" + rp.group(2),
                     "name": rp.group(2), "slug": rp.group(2)}
    if re.search(r"/commit/[0-9a-f]+/reports/[^/]+/annotations$", p):
        return 200, {"values": []}
    if re.search(r"/commit/[0-9a-f]+/reports/[^/]+$", p):
        return (204, None) if m == "DELETE" else (200, {"uuid": "{r1}", "result": "FAILED"})
    if re.search(r"/commit/[0-9a-f]+/statuses/build$", p):
        return 201, {"key": "SonarQube", "state": "FAILED"}
    return None


ROUTES = [gitlab, azure, bitbucket_server, github, bitbucket_cloud]
# Bitbucket Cloud is reached through its real host names; prefix its paths so routing stays path based.
HOST_PREFIX = {"bitbucket.org": "/bbc-auth", "api.bitbucket.org": "/bbc"}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def handle_any(self):
        url = urlsplit(self.path)
        query = parse_qs(url.query)
        host = (self.headers.get("Host") or "").split(":")[0]
        path = HOST_PREFIX.get(host, "") + url.path
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length).decode("utf-8", "replace") if length else ""
        result = None
        for route in ROUTES:
            result = route(self.command, path, query, raw)
            if result:
                break
        status, payload = result or (200, {})
        record = {"t": time.time(), "method": self.command, "host": host, "path": path, "query": url.query,
                  "auth": bool(self.headers.get("Authorization") or self.headers.get("PRIVATE-TOKEN")),
                  "content_type": self.headers.get("Content-Type"), "body": raw, "status": status,
                  "matched": result is not None}
        with LOCK, open(LOG, "a") as log:
            log.write(json.dumps(record) + "\n")
        data = b"" if payload is None else json.dumps(payload).replace(
            "http://GHHOST", "http://" + (self.headers.get("Host") or "localhost")).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    do_GET = do_POST = do_PUT = do_PATCH = do_DELETE = handle_any

    def log_message(self, *args):
        pass


def serve(port, tls=False):
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    if tls:
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(os.environ["TLS_CERT"], os.environ["TLS_KEY"])
        server.socket = context.wrap_socket(server.socket, server_side=True)
    server.serve_forever()


if __name__ == "__main__":
    threading.Thread(target=serve, args=(443, True), daemon=True).start()
    print("alm-mock listening on :8080 (http) and :443 (https)", flush=True)
    serve(8080)
