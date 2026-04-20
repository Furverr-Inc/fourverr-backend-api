package com.fourverr.api.service;

import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.PaymentIntent;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${stripe.connect.return-url:https://furverr-inc.github.io/fourverr-web/perfil}")
    private String connectReturnUrl;

    @Value("${stripe.connect.refresh-url:https://furverr-inc.github.io/fourverr-web/perfil}")
    private String connectRefreshUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    // ─────────────── PAGOS BÁSICOS ───────────────

    /**
     * Crea un PaymentIntent estándar (sin Connect).
     * Se usa cuando el vendedor aún NO tiene cuenta de Stripe Connect.
     */
    public PaymentIntent crearPaymentIntent(BigDecimal precio, String currency) throws Exception {
        long monto = precio.multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(monto)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Crea un PaymentIntent con Stripe Connect (destination charge).
     * El 90% del monto va directo a la cuenta del vendedor; el 10% queda en la plataforma.
     *
     * @param precio          Precio total del pedido
     * @param currency        "mxn" o "usd"
     * @param stripeAccountId ID de la cuenta conectada del vendedor (acct_...)
     */
    public PaymentIntent crearPaymentIntentConnect(BigDecimal precio, String currency, String stripeAccountId) throws Exception {
        long montoTotal   = precio.multiply(BigDecimal.valueOf(100)).longValue();
        // 10% de la comisión de la plataforma en la misma unidad que amount (centavos / minor units)
        long comisionPlat = montoTotal / 10;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(montoTotal)
                .setCurrency(currency)
                .setApplicationFeeAmount(comisionPlat)
                .setTransferData(
                    PaymentIntentCreateParams.TransferData.builder()
                        .setDestination(stripeAccountId)
                        .build()
                )
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

        return PaymentIntent.create(params);
    }

    /** Verifica que un PaymentIntent realmente fue pagado */
    public boolean verificarPago(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(intent.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────── STRIPE CONNECT — ONBOARDING ───────────────

    /**
     * Crea una cuenta Express de Stripe Connect para el vendedor.
     * @param email  Email del vendedor
     * @return       ID de la cuenta (acct_...)
     */
    public String crearCuentaConnect(String email) throws Exception {
        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setEmail(email)
                .setCapabilities(
                    AccountCreateParams.Capabilities.builder()
                        .setCardPayments(
                            AccountCreateParams.Capabilities.CardPayments.builder()
                                .setRequested(true).build()
                        )
                        .setTransfers(
                            AccountCreateParams.Capabilities.Transfers.builder()
                                .setRequested(true).build()
                        )
                        .build()
                )
                .build();

        Account account = Account.create(params);
        return account.getId();
    }

    /**
     * Genera el link de onboarding para que el vendedor complete su perfil en Stripe.
     * @param stripeAccountId  ID de la cuenta Express (acct_...)
     * @return                 URL a la que redirigir al vendedor
     */
    public String generarLinkOnboarding(String stripeAccountId) throws Exception {
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(stripeAccountId)
                .setReturnUrl(connectReturnUrl)
                .setRefreshUrl(connectRefreshUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink link = AccountLink.create(params);
        return link.getUrl();
    }

    /**
     * Verifica si la cuenta Express del vendedor completó el onboarding
     * (charges_enabled = true significa que puede recibir pagos).
     */
    public boolean cuentaHabilitada(String stripeAccountId) {
        try {
            Account account = Account.retrieve(stripeAccountId);
            return Boolean.TRUE.equals(account.getChargesEnabled());
        } catch (Exception e) {
            return false;
        }
    }
}
