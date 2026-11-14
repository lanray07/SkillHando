"""Amazon RVS verifier behind a TLS reverse proxy. Rate-limit POST /verify at that proxy."""
import json
import os
import logging
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

SECRET = os.environ.get("SKILLHANDO_RVS_SECRET", "")
PRODUCT = os.environ.get("SKILLHANDO_RVS_PRODUCT", "")
MODE = os.environ.get("SKILLHANDO_RVS_MODE", "production")
ENDPOINTS = {
    "production": "https://appstore-sdk.amazon.com/version/1.0/verifyReceiptId",
    "sandbox": "https://appstore-sdk.amazon.com/sandbox/version/1.0/verifyReceiptId",
}
TOKEN = re.compile(r"[A-Za-z0-9_\-+/=.:]{1,1024}")


class Verify(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def reply(self, status, valid=False):
        if MODE == "sandbox":
            logging.info("sandbox verifier response status=%s valid=%s", status, valid)
        body = json.dumps({"valid": valid}).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        if self.path != "/verify":
            return self.reply(404)
        self.connection.settimeout(8)
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length not in range(1, 4097):
                return self.reply(400)
            data = json.loads(self.rfile.read(length))
            user, receipt = data["userId"], data["receiptId"]
            if not all(isinstance(x, str) and TOKEN.fullmatch(x) for x in (user, receipt)):
                return self.reply(400)
        except (ValueError, KeyError, TypeError, TimeoutError):
            return self.reply(400)
        try:
            url = ENDPOINTS[MODE] + "/developer/" + quote(SECRET, safe="") + "/user/" + quote(user, safe="") + "/receiptId/" + quote(receipt, safe="")
            with urlopen(Request(url, headers={"Accept": "application/json"}), timeout=5) as result:
                verified = json.loads(result.read(32_768))
            self.reply(200, verified.get("productId") == PRODUCT and verified.get("productType") == "ENTITLED"
                       and verified.get("receiptId") == receipt and "cancelDate" in verified and verified["cancelDate"] is None)
        except HTTPError as error:
            self.reply(200 if error.code in (400, 410) else 503)
        except (URLError, ValueError, TimeoutError):
            self.reply(503)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    if not SECRET or not PRODUCT or MODE not in ENDPOINTS:
        raise SystemExit("Set SKILLHANDO_RVS_SECRET, SKILLHANDO_RVS_PRODUCT and a valid SKILLHANDO_RVS_MODE.")
    ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("SKILLHANDO_RVS_PORT", "8080"))), Verify).serve_forever()
