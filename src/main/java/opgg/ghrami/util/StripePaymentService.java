package opgg.ghrami.util;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Service for Stripe Checkout Session management.
 *
 * Setup:
 *   1. API keys are configured below (test mode).
 *   2. Run: mvn clean install  (pulls stripe-java 25.12.0)
 *   3. Test cards: https://stripe.com/docs/testing
 *      - Success: 4242 4242 4242 4242 | any future date | any 3-digit CVC
 *      - Decline:  4000 0000 0000 0002
 */
public class StripePaymentService {

    // Stripe SECRET key — never expose this on the client side or commit to version control.
    private static final String API_KEY = "sk_test_51T4HU4IE7HmerKSHDYxaMmhGCuh9ItO6lRXTCji7nyLDItSqyzFaYaDCKEB46m5bzMKkFO6XkjrWYRiNwjtHPRr400mWfOiDuf";

    // Stripe PUBLISHABLE key — safe for client-side use only (not used server-side here).
    public static final String PUBLISHABLE_KEY = "pk_test_51T4HU4IE7HmerKSHdza63qLl2EgOlJEHhLxJxlg7sLTM3znld2RWwp2V2lbUnj358TNPaUDNkCa6cGdJnXfHo95t00jLMawfLb";

    /**
     * Internal redirect URLs intercepted by the JavaFX WebView location listener.
     * No actual HTTP server is needed — we just detect the URL change in WebView.
     */
    public static final String SUCCESS_BASE = "http://localhost:15555/stripe/success";
    public static final String CANCEL_URL   = "http://localhost:15555/stripe/cancel";

    /**
     * Creates a Stripe Checkout Session.
     *
     * @param className    product name shown on Stripe's hosted page
     * @param amountCents  amount in the smallest currency unit (e.g., cents for EUR)
     * @param currency     ISO currency code, e.g. "eur", "usd"
     * @param bookingId    embedded in the success URL so we know which booking to confirm
     * @return the Stripe Session object (use session.getUrl() to get the checkout URL)
     */
    public static Session createCheckoutSession(String className,
                                                long amountCents,
                                                String currency,
                                                Long bookingId) throws StripeException {
        Stripe.apiKey = API_KEY;

        String successUrl = SUCCESS_BASE
                + "?session_id={CHECKOUT_SESSION_ID}&booking_id=" + bookingId;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(successUrl)
                .setCancelUrl(CANCEL_URL)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(className)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        return Session.create(params);
    }

    /**
     * Verifies that the Stripe Checkout Session actually resulted in a completed payment.
     */
    public static boolean verifySessionPaid(String sessionId) throws StripeException {
        Stripe.apiKey = API_KEY;
        Session session = Session.retrieve(sessionId);
        return "paid".equals(session.getPaymentStatus());
    }

    /**
     * Returns false if the API key is still the default placeholder.
     */
    public static boolean isConfigured() {
        return API_KEY != null
                && !API_KEY.isEmpty()
                && !API_KEY.equals("sk_test_YOUR_KEY_HERE");
    }

    /**
     * Converts a TND (Tunisian Dinar) amount to EUR cents for Stripe.
     * Note: Stripe does not natively support TND.
     * Using approximate rate 1 TND ≈ 0.30 EUR. Replace with a live rate in production.
     * Minimum is 50 cents (Stripe's minimum charge).
     */
    public static long tndToEurCents(double amountTnd) {
        double eurAmount = amountTnd * 0.30;
        return Math.max(50L, Math.round(eurAmount * 100));
    }
}