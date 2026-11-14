import assert from "node:assert/strict";
import { test } from "node:test";
import worker from "./worker.mjs";

test("production verifier grants only matching, uncanceled production receipts", async (t) => {
    const env = {
        SKILLHANDO_RVS_SECRET: "fixture",
        VERIFY_RATE_LIMIT: { limit: async () => ({ success: true }) },
    };
    const purchase = { userId: "user=", receiptId: "receipt=:1:11" };
    const verify = (data = purchase) => worker.fetch(new Request("https://test.example/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
    }), env);
    assert.equal((await verify({ ...purchase, receiptId: "x".repeat(4096) })).status, 400);
    const receipt = {
        productId: "com.skillhando.app.premium", productType: "ENTITLED",
        receiptId: purchase.receiptId, cancelDate: null, testTransaction: false, betaProduct: false,
    };
    let amazon = Response.json(receipt);
    t.mock.method(globalThis, "fetch", async (url, options) => {
        assert.match(url, /^https:\/\/appstore-sdk\.amazon\.com\/version\/1\.0\/verifyReceiptId\/developer\/fixture\//);
        assert.match(url, /\/user\/user%3D\/receiptId\/receipt%3D%3A1%3A11$/);
        assert.equal(options.redirect, "manual");
        return amazon;
    });
    assert.deepEqual(await (await verify()).json(), { valid: true });
    for (const overrides of [
        { cancelDate: 1 }, { testTransaction: true }, { betaProduct: true },
        { productId: "com.skillhando.testing.premium" }, { receiptId: "wrong" },
        { productType: "CONSUMABLE" },
    ]) {
        amazon = Response.json({ ...receipt, ...overrides });
        assert.deepEqual(await (await verify()).json(), { valid: false });
    }
    amazon = new Response(null, { status: 410 });
    const revoked = await verify();
    assert.equal(revoked.status, 200);
    assert.deepEqual(await revoked.json(), { valid: false });
    amazon = new Response(null, { status: 496 });
    assert.equal((await verify()).status, 503);
    env.SKILLHANDO_LIVE_APP_TEST = "true";
    amazon = Response.json({ ...receipt });
    assert.deepEqual(await (await verify()).json(), { valid: false });
    const betaReceipt = { ...receipt, productId: "com.skillhando.app.premium.lat",
        testTransaction: false, betaProduct: true };
    amazon = Response.json(betaReceipt);
    assert.deepEqual(await (await verify()).json(), { valid: true });
    amazon = Response.json({ ...betaReceipt, testTransaction: true, betaProduct: false });
    assert.deepEqual(await (await verify()).json(), { valid: false });
    env.VERIFY_RATE_LIMIT.limit = async () => ({ success: false });
    assert.equal((await verify()).status, 429);
});
