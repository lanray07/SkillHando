const sku = "com.skillhando.app.premium";
const amazonUrl = "https://appstore-sdk.amazon.com/version/1.0/verifyReceiptId";
const token = /^[A-Za-z0-9_\-+/=.:]{1,1024}$/;
const responseHeaders = { "Cache-Control": "no-store" };

function decision(status, valid = false) {
    return Response.json({ valid }, { status, headers: responseHeaders });
}

async function readRequest(request) {
    if (request.headers.get("Content-Type")?.split(";")[0].trim() !== "application/json" || !request.body) {
        return null;
    }
    const reader = request.body.getReader();
    const decoder = new TextDecoder("utf-8", { fatal: true });
    let bytes = 0;
    let body = "";
    try {
        for (;;) {
            const { done, value } = await reader.read();
            if (done) break;
            bytes += value.byteLength;
            if (bytes > 4096) return null;
            body += decoder.decode(value, { stream: true });
        }
        const data = JSON.parse(body + decoder.decode());
        if (typeof data?.userId !== "string" || typeof data.receiptId !== "string") return null;
        if (!token.test(data.userId) || !token.test(data.receiptId)) return null;
        return data;
    } catch {
        return null;
    } finally {
        await reader.cancel().catch(() => {});
    }
}

export default {
    async fetch(request, env) {
        const { protocol, pathname: path } = new URL(request.url);
        if (protocol !== "https:") return decision(426);
        if (path === "/health" && request.method === "GET") {
            return new Response(null, {
                status: env.SKILLHANDO_RVS_SECRET && env.VERIFY_RATE_LIMIT ? 204 : 503,
                headers: responseHeaders,
            });
        }
        if (path !== "/verify") return decision(404);
        if (request.method !== "POST") return decision(405);
        const secret = env.SKILLHANDO_RVS_SECRET?.trim();
        if (!secret || !env.VERIFY_RATE_LIMIT) return decision(503);

        const key = request.headers.get("CF-Connecting-IP") || "unknown";
        const { success } = await env.VERIFY_RATE_LIMIT.limit({ key });
        if (!success) return decision(429);

        const purchase = await readRequest(request);
        if (!purchase) return decision(400);
        const { userId, receiptId } = purchase;
        const url = amazonUrl + "/developer/" + encodeURIComponent(secret) +
            "/user/" + encodeURIComponent(userId) + "/receiptId/" + encodeURIComponent(receiptId);

        try {
            const result = await fetch(url, {
                headers: { Accept: "application/json" },
                redirect: "manual",
                signal: AbortSignal.timeout(5000),
            });
            if ([400, 410, 497].includes(result.status)) return decision(200);
            if (result.status !== 200) return decision(503);
            const receipt = await result.json();
            return decision(200, receipt?.productId === sku && receipt.productType === "ENTITLED" &&
                receipt.receiptId === receiptId && receipt.cancelDate === null &&
                receipt.testTransaction === false && receipt.betaProduct !== true);
        } catch {
            return decision(503);
        }
    },
};
