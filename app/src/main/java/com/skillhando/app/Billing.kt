package com.skillhando.app

import android.content.Context
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class BillingState(val price: String? = null, val active: Boolean = false, val message: String = "", val working: Boolean = false)

object Billing : PurchasingListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sku = BuildConfig.AMAZON_SKU
    private val endpoint = BuildConfig.VERIFIER_URL
    val configured = sku.isNotBlank() && endpoint.startsWith("https://")
    val state = MutableStateFlow(BillingState())
    private var registered = false
    private var context: Context? = null
    private var account: String? = null
    private var restoreRequest: RequestId? = null
    private var purchaseRequest: RequestId? = null
    private val restored = mutableListOf<Receipt>()

    fun open(context: Context) {
        if (!configured || registered) return
        this.context = context.applicationContext
        runStore {
            PurchasingService.registerListener(context.applicationContext, this)
            registered = true
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(sku))
        }
    }

    fun buy() = runStore {
        if (configured && !state.value.working && state.value.price != null && !state.value.active) {
            working(tr("opening_amazon_checkout"))
            purchaseRequest = PurchasingService.purchase(sku)
        }
    }

    fun restore() = runStore {
        if (configured && !state.value.working) {
            restored.clear()
            working(tr("checking_your_amazon_purchases"))
            restoreRequest = PurchasingService.getPurchaseUpdates(true)
        }
    }

    override fun onUserDataResponse(response: UserDataResponse) = onMain {
        if (response.requestStatus != UserDataResponse.RequestStatus.SUCCESSFUL) return@onMain
        setAccount(response.userData.userId)
        if (!state.value.working) restore()
    }

    override fun onProductDataResponse(response: ProductDataResponse) = onMain {
        val product = response.productData?.get(sku)?.takeIf { it.productType == ProductType.ENTITLED }
        state.value = state.value.copy(price = product?.price,
            message = if (product == null) tr("amazon_pricing_is_unavailable_for_this_account_or_device") else state.value.message)
    }

    override fun onPurchaseResponse(response: PurchaseResponse) = onMain {
        if (response.requestId != purchaseRequest) return@onMain
        purchaseRequest = null
        when (response.requestStatus.name) {
            "SUCCESSFUL" -> {
                val user = response.userData.userId
                setAccount(user)
                validate(user, listOf(response.receipt), false)
            }
            "ALREADY_PURCHASED" -> { finish(""); restore() }
            "INVALID_SKU", "NOT_SUPPORTED" -> finish(tr("this_product_isn_t_available_on_this_device"))
            else -> finish(tr("amazon_didn_t_complete_the_purchase_you_can_restore_a_previous_purchase"))
        }
    }

    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) = onMain {
        if (response.requestId != restoreRequest) return@onMain
        restoreRequest = null
        if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) {
            finish(tr("amazon_couldn_t_check_your_purchases_try_again_when_connected"))
            return@onMain
        }
        val user = response.userData.userId
        if (account != null && account != user) restored.clear()
        setAccount(user)
        restored.addAll(response.receipts)
        if (response.hasMore()) runStore { restoreRequest = PurchasingService.getPurchaseUpdates(false) }
        else {
            val batch = restored.toList()
            restored.clear()
            validate(user, batch, true)
        }
    }

    private fun onMain(block: () -> Unit) { scope.launch { block() } }

    private fun setAccount(user: String) {
        if (account == user) return
        account = user
        val saved = context?.getSharedPreferences("amazon-entitlement", 0)
        val valid = saved?.getString("account", null) == user &&
            (saved?.getLong("expires", 0L) ?: 0L) > System.currentTimeMillis()
        state.value = state.value.copy(active = valid)
    }

    private fun validate(user: String, receipts: List<Receipt>, fullRestore: Boolean) {
        val candidates = receipts.filter { it.sku == sku && it.productType == ProductType.ENTITLED && !it.isCanceled }
        scope.launch {
            try {
                val verified = withContext(Dispatchers.IO) {
                    candidates.firstOrNull { receipt -> verifyReceipt(user, receipt.receiptId) }
                }
                if (account != user) { finish(tr("your_amazon_account_changed_restore_purchases_to_continue")); return@launch }
                if (verified != null || fullRestore || candidates.isNotEmpty()) {
                    val prefs = context?.getSharedPreferences("amazon-entitlement", 0)
                    prefs?.edit()?.putString("account", user)?.putLong("expires",
                        if (verified != null) System.currentTimeMillis() + 7 * 86_400_000L else 0L)?.apply()
                    state.value = state.value.copy(active = verified != null)
                }
                if (verified != null) runStore { PurchasingService.notifyFulfillment(verified.receiptId, FulfillmentResult.FULFILLED) }
                finish(if (verified != null) tr("premium_purchase_verified") else tr("no_verified_premium_purchase_was_found"))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { finish(tr("purchase_verification_is_temporarily_unavailable_try_restore_purchases_again")) }
        }
    }

    private fun verifyReceipt(user: String, receipt: String): Boolean {
        val connection = (URL(endpoint).openConnection() as HttpsURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
        }
        try {
            val body = JSONObject().put("userId", user).put("receiptId", receipt).toString()
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            check(connection.responseCode == 200)
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getBoolean("valid")
        } finally { connection.disconnect() }
    }

    private fun working(message: String) { state.value = state.value.copy(working = true, message = message) }
    private fun finish(message: String) { state.value = state.value.copy(working = false, message = message) }
    private inline fun runStore(call: () -> Unit) {
        try { call() } catch (_: RuntimeException) { finish(tr("amazon_purchasing_is_unavailable_right_now")) }
    }
}
