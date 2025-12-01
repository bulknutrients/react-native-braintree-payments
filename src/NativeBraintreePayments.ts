import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface CardDetails {
  number: string;
  expirationMonth: string;
  expirationYear: string;
  cvv: string;
  cardholderName?: string;
  postalCode?: string;
}

export interface PaymentMethodNonce {
  nonce: string;
  isDefault: boolean;
  type: string;
  cardType?: string;
  lastTwo?: string;
  lastFour?: string;
  bin?: string;
  expirationMonth?: string;
  expirationYear?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

export interface PayPalCheckoutOptions {
  currencyCode?: string;
  intent?: 'sale' | 'authorize';
  displayName?: string;
}

export interface PayPalVaultOptions {
  displayName?: string;
}

export interface Spec extends TurboModule {
  initialize(clientToken: string): Promise<boolean>;
  tokenizeCard(cardDetails: CardDetails): Promise<PaymentMethodNonce>;
  requestPayPalCheckout(
    amount: string,
    options?: PayPalCheckoutOptions
  ): Promise<PaymentMethodNonce>;
  requestPayPalVault(options?: PayPalVaultOptions): Promise<PaymentMethodNonce>;
  collectDeviceData(): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('BraintreePayments');