import { NativeModules, Platform } from 'react-native';
import NativeBraintreePayments from './NativeBraintreePayments';

export type {
  CardDetails,
  PaymentMethodNonce,
  PayPalCheckoutOptions,
  PayPalVaultOptions,
} from './NativeBraintreePayments';

const LINKING_ERROR =
  `The package 'react-native-braintree-payments' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- Run 'pod install'\n", default: '' }) +
  '- Rebuild the app after installing the package\n' +
  '- If using Expo, run npx expo prebuild\n';

const BraintreePayments =
  NativeBraintreePayments ?? NativeModules.BraintreePayments
    ? NativeModules.BraintreePayments
    : new Proxy(
        {},
        {
          get() {
            throw new Error(LINKING_ERROR);
          },
        }
      );

export default BraintreePayments;