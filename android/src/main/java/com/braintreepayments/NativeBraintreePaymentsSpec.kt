package com.braintreepayments

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap

abstract class NativeBraintreePaymentsSpec(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  abstract fun initialize(clientToken: String, promise: Promise)
  
  abstract fun tokenizeCard(cardDetails: ReadableMap, promise: Promise)
  
  abstract fun requestPayPalCheckout(
    amount: String, 
    options: ReadableMap?, 
    promise: Promise
  )
  
  abstract fun requestPayPalVault(
    options: ReadableMap?, 
    promise: Promise
  )
  
  abstract fun collectDeviceData(promise: Promise)
}