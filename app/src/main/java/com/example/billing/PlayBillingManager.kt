package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.remote.ApiClient
import com.example.data.remote.models.PlayPurchaseVerifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlayProductItem(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val productDetails: ProductDetails
)

class PlayBillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onPurchaseVerified: (sms: Int, min: Int, assignedNumber: String?) -> Unit
) : PurchasesUpdatedListener {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<PlayProductItem>>(emptyList())
    val availableProducts: StateFlow<List<PlayProductItem>> = _availableProducts.asStateFlow()

    private val _billingStatusMessage = MutableStateFlow("Google Play Billing Initializing...")
    val billingStatusMessage: StateFlow<String> = _billingStatusMessage.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isReady.value = true
                    _billingStatusMessage.value = "Connected to Google Play Billing"
                    queryProducts()
                } else {
                    _isReady.value = false
                    _billingStatusMessage.value = "Billing Setup Error: ${billingResult.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                _isReady.value = false
                _billingStatusMessage.value = "Google Play Disconnected. Reconnecting..."
                // Try reconnection
                startConnection()
            }
        })
    }

    fun queryProducts() {
        if (!_isReady.value) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pkg_temp_burner_otp")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pkg_us_call_sms")
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("pkg_wa_verification")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val items = queryProductDetailsList.map { pd ->
                    val offer = pd.oneTimePurchaseOfferDetails
                    val formattedPrice = offer?.formattedPrice ?: "$0.99"
                    PlayProductItem(
                        productId = pd.productId,
                        title = pd.title,
                        description = pd.description,
                        formattedPrice = formattedPrice,
                        productDetails = pd
                    )
                }
                _availableProducts.value = items
            } else {
                _billingStatusMessage.value = "Product Query Code: ${billingResult.responseCode}"
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _billingStatusMessage.value = "Purchase cancelled by user"
        } else {
            _billingStatusMessage.value = "Purchase error: ${billingResult.debugMessage}"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val productId = purchase.products.firstOrNull() ?: "pkg_us_call_sms"
                    val verifyReq = PlayPurchaseVerifyRequest(
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                        orderId = purchase.orderId
                    )
                    val response = ApiClient.getService().verifyPlayPurchase(verifyReq)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val body = response.body()!!
                        withContext(Dispatchers.Main) {
                            onPurchaseVerified(body.smsBalance, body.callMinutesBalance, body.assignedNumber)
                        }

                        // Consume or Acknowledge
                        val consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.consumeAsync(consumeParams) { _, _ -> }
                    }
                } catch (e: Exception) {
                    _billingStatusMessage.value = "Verification error: ${e.message}"
                }
            }
        }
    }
}
