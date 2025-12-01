package com.braintreepayments

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BraintreeClient
import com.braintreepayments.api.Card
import com.braintreepayments.api.CardClient
import com.braintreepayments.api.CardNonce
import com.braintreepayments.api.CardTokenizeCallback
import com.braintreepayments.api.DataCollector
import com.braintreepayments.api.DataCollectorCallback
import com.braintreepayments.api.PayPalAccountNonce
import com.braintreepayments.api.PayPalClient
import com.braintreepayments.api.PayPalListener
import com.braintreepayments.api.PayPalCheckoutRequest
import com.braintreepayments.api.PayPalVaultRequest
import com.braintreepayments.api.PaymentMethodNonce
import com.braintreepayments.api.UserCanceledException
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = BraintreePaymentsModule.NAME)
class BraintreePaymentsModule(reactContext: ReactApplicationContext) :
  NativeBraintreePaymentsSpec(reactContext) {

  private var braintreeClient: BraintreeClient? = null
  private var cardClient: CardClient? = null
  private var payPalClient: PayPalClient? = null
  private var dataCollector: DataCollector? = null
  
  private var payPalPromise: Promise? = null

  override fun getName(): String = NAME

  @ReactMethod
  override fun initialize(clientToken: String, promise: Promise) {
    try {
      currentActivity?.runOnUiThread {
        try {
          braintreeClient = BraintreeClient(reactApplicationContext, clientToken)
          cardClient = CardClient(braintreeClient!!)
          payPalClient = PayPalClient(braintreeClient!!)
          dataCollector = DataCollector(braintreeClient!!)
          
          setupPayPalListener()
          
          promise.resolve(true)
        } catch (e: Exception) {
          promise.reject("INITIALIZATION_ERROR", e.message, e)
        }
      }
    } catch (e: Exception) {
      promise.reject("INITIALIZATION_ERROR", e.message, e)
    }
  }
  
  private fun setupPayPalListener() {
    payPalClient?.setListener(object : PayPalListener {
      override fun onPayPalSuccess(payPalAccountNonce: PayPalAccountNonce) {
        val result = createPaymentMethodNonceMap(payPalAccountNonce)
        payPalPromise?.resolve(result)
        payPalPromise = null
      }

      override fun onPayPalFailure(error: Exception) {
        if (error is UserCanceledException) {
          payPalPromise?.reject("USER_CANCELLED", "User cancelled PayPal")
        } else {
          payPalPromise?.reject("PAYPAL_ERROR", error.message, error)
        }
        payPalPromise = null
      }
    })
  }

  @ReactMethod
  override fun tokenizeCard(cardDetails: ReadableMap, promise: Promise) {
    try {
      if (cardClient == null) {
        promise.reject("NOT_INITIALIZED", "Braintree client not initialized")
        return
      }

      val card = Card().apply {
        number = cardDetails.getString("number")
        expirationMonth = cardDetails.getString("expirationMonth")
        expirationYear = cardDetails.getString("expirationYear")
        cvv = cardDetails.getString("cvv")
        
        if (cardDetails.hasKey("cardholderName")) {
          cardholderName = cardDetails.getString("cardholderName")
        }
        if (cardDetails.hasKey("postalCode")) {
          postalCode = cardDetails.getString("postalCode")
        }
      }

      cardClient?.tokenize(card, object : CardTokenizeCallback {
        override fun onResult(cardNonce: CardNonce?, error: Exception?) {
          if (error != null) {
            promise.reject("TOKENIZATION_ERROR", error.message, error)
          } else if (cardNonce != null) {
            val result = createPaymentMethodNonceMap(cardNonce)
            promise.resolve(result)
          } else {
            promise.reject("TOKENIZATION_ERROR", "Unknown error occurred")
          }
        }
      })
    } catch (e: Exception) {
      promise.reject("TOKENIZATION_ERROR", e.message, e)
    }
  }

  @ReactMethod
  override fun requestPayPalCheckout(amount: String, options: ReadableMap?, promise: Promise) {
    try {
      if (payPalClient == null) {
        promise.reject("NOT_INITIALIZED", "Braintree client not initialized")
        return
      }

      val activity = currentActivity
      if (activity !is FragmentActivity) {
        promise.reject("ACTIVITY_ERROR", "Current activity must be a FragmentActivity")
        return
      }

      payPalPromise = promise

      val request = PayPalCheckoutRequest(amount).apply {
        currencyCode = options?.getString("currencyCode") ?: "USD"
        intent = if (options?.getString("intent") == "sale") {
          PayPalCheckoutRequest.INTENT_SALE
        } else {
          PayPalCheckoutRequest.INTENT_AUTHORIZE
        }
        
        if (options?.hasKey("displayName") == true) {
          displayName = options.getString("displayName")
        }
      }

      payPalClient?.tokenizePayPalAccount(activity, request)
    } catch (e: Exception) {
      promise.reject("PAYPAL_ERROR", e.message, e)
      payPalPromise = null
    }
  }

  @ReactMethod
  override fun requestPayPalVault(options: ReadableMap?, promise: Promise) {
    try {
      if (payPalClient == null) {
        promise.reject("NOT_INITIALIZED", "Braintree client not initialized")
        return
      }

      val activity = currentActivity
      if (activity !is FragmentActivity) {
        promise.reject("ACTIVITY_ERROR", "Current activity must be a FragmentActivity")
        return
      }

      payPalPromise = promise

      val request = PayPalVaultRequest().apply {
        if (options?.hasKey("displayName") == true) {
          displayName = options.getString("displayName")
        }
      }

      payPalClient?.tokenizePayPalAccount(activity, request)
    } catch (e: Exception) {
      promise.reject("PAYPAL_ERROR", e.message, e)
      payPalPromise = null
    }
  }

  @ReactMethod
  override fun collectDeviceData(promise: Promise) {
    try {
      if (dataCollector == null) {
        promise.reject("NOT_INITIALIZED", "Braintree client not initialized")
        return
      }

      dataCollector?.collectDeviceData(reactApplicationContext, object : DataCollectorCallback {
        override fun onResult(deviceData: String?, error: Exception?) {
          if (error != null) {
            promise.reject("DEVICE_DATA_ERROR", error.message, error)
          } else {
            promise.resolve(deviceData ?: "")
          }
        }
      })
    } catch (e: Exception) {
      promise.reject("DEVICE_DATA_ERROR", e.message, e)
    }
  }

  private fun createPaymentMethodNonceMap(nonce: PaymentMethodNonce): WritableMap {
    return Arguments.createMap().apply {
      putString("nonce", nonce.string)
      putBoolean("isDefault", nonce.isDefault)
      
      when (nonce) {
        is CardNonce -> {
          putString("type", "Card")
          putString("cardType", nonce.cardType)
          putString("lastTwo", nonce.lastTwo)
          putString("lastFour", nonce.lastFour)
          
          nonce.bin?.let { putString("bin", it) }
          nonce.expirationMonth?.let { putString("expirationMonth", it) }
          nonce.expirationYear?.let { putString("expirationYear", it) }
        }
        is PayPalAccountNonce -> {
          putString("type", "PayPal")
          nonce.email?.let { putString("email", it) }
          nonce.firstName?.let { putString("firstName", it) }
          nonce.lastName?.let { putString("lastName", it) }
        }
        else -> {
          putString("type", nonce.javaClass.simpleName)
        }
      }
    }
  }

  companion object {
    const val NAME = "BraintreePayments"
  }
}