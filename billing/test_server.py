import json
from http.server import ThreadingHTTPServer
from threading import Thread
from unittest import TestCase, main
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

import server


class ReceiptVerificationTest(TestCase):
    def setUp(self):
        self.http = ThreadingHTTPServer(("127.0.0.1", 0), server.Verify)
        self.thread = Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()
        self.addCleanup(self.http.server_close)
        self.addCleanup(self.thread.join)
        self.addCleanup(self.http.shutdown)
        self.config = patch.multiple(server, SECRET="test", PRODUCT="test.premium", MODE="sandbox")
        self.config.start()
        self.addCleanup(self.config.stop)

    def verify(self, data):
        url = f"http://127.0.0.1:{self.http.server_port}/verify"
        request = Request(url, json.dumps(data).encode(), headers={"Content-Type": "application/json"})
        with urlopen(request, timeout=3) as response:
            return json.load(response)["valid"]

    def amazon_response(self, **changes):
        content = dict(productId="test.premium", productType="ENTITLED", receiptId="example=:2:11", cancelDate=None)
        content.update(changes)
        connection = MagicMock()
        connection.__enter__.return_value.read.return_value = json.dumps(content).encode()
        return connection

    def test_valid_entitlement_and_sandbox_address(self):
        with patch.object(server, "urlopen", return_value=self.amazon_response()) as amazon:
            self.assertTrue(self.verify({"userId": "user=", "receiptId": "example=:2:11"}))
            self.assertIn("/sandbox/version/1.0/verifyReceiptId/developer/test/", amazon.call_args.args[0].full_url)

    def test_refunded_or_mismatched_purchase_is_invalid(self):
        request = {"userId": "user=", "receiptId": "example=:2:11"}
        for changes in ({"cancelDate": 42}, {"receiptId": "different"}, {"productId": "other"}):
            with self.subTest(changes=changes), patch.object(server, "urlopen", return_value=self.amazon_response(**changes)):
                self.assertFalse(self.verify(request))

    def test_rejects_bad_identifier_without_contacting_amazon(self):
        with patch.object(server, "urlopen") as amazon:
            with self.assertRaises(HTTPError) as error:
                self.verify({"userId": "invalid user", "receiptId": "example=:2:11"})
            self.assertEqual(400, error.exception.code)
            amazon.assert_not_called()


if __name__ == "__main__":
    main()
