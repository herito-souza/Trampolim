package com.trampolim.api.company;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Slf4j
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class CompanyPaymentController {
    private final JdbcTemplate jdbc;
    private final RestClient mercadopago = RestClient.builder().baseUrl("https://api.mercadopago.com").build();
    @Value("${payments.mercadopago.access-token:}") private String accessToken;
    @Value("${payments.mercadopago.webhook-secret:}") private String webhookSecret;

    public CompanyPaymentController(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record PaymentCheckout(long listingId, int amountCents, String currency, String status, String checkoutUrl) {}
    public record PaymentHistory(long id, int amountCents, String provider, String status, String providerStatus, String checkoutUrl, String createdAt, String paidAt) {}
    public record WebhookEvent(String type, JsonNode data) {}

    @PostMapping("/api/company/listings/{listingId}/payment")
    public PaymentCheckout createCheckout(@AuthenticationPrincipal String email, @PathVariable long listingId) {
        long companyId = requireCompanyWriter(email);
        List<Object[]> rows = jdbc.query("SELECT is_internship,price_cents,payment_status,publication_status,title FROM company_listings WHERE id=? AND company_id=?", (rs, row) -> new Object[]{rs.getBoolean(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5)}, listingId, companyId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Anúncio não encontrado.");
        if (accessToken == null || accessToken.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pagamentos ainda não configurados. A empresa pode salvar a vaga e concluir o pagamento quando a integração for ativada.");
        Object[] listing = rows.get(0);
        if (!(boolean) listing[0]) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O pagamento se aplica somente a vagas de estágio.");
        if ((int) listing[1] != 5000) throw new ResponseStatusException(HttpStatus.CONFLICT, "O valor da divulgação não corresponde à tarifa de estágio.");
        if ("PAID".equals(listing[2])) return latestCheckout(listingId, (int) listing[1]);

        List<Object[]> pending = jdbc.query("SELECT id,external_reference,idempotency_key,provider_order_id,checkout_url,status FROM company_listing_payments WHERE listing_id=? AND status IN ('CREATING','PENDING') ORDER BY created_at DESC LIMIT 1", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5), rs.getString(6)}, listingId);
        Object[] payment;
        if (pending.isEmpty()) {
            UUID key = UUID.randomUUID(); String externalReference = UUID.randomUUID().toString().replace("-", "");
            try {
                Long id = jdbc.queryForObject("INSERT INTO company_listing_payments(listing_id,company_id,external_reference,idempotency_key,amount_cents,status) VALUES (?,?,?,?,5000,'CREATING') RETURNING id", Long.class, listingId, companyId, externalReference, key);
                payment = new Object[]{id, externalReference, key, null, null, "CREATING"};
                jdbc.update("UPDATE company_listings SET payment_status='PENDING',publication_status='AWAITING_PAYMENT',updated_at=CURRENT_TIMESTAMP WHERE id=?", listingId);
            } catch (org.springframework.dao.DuplicateKeyException concurrent) {
                payment = jdbc.queryForObject("SELECT id,external_reference,idempotency_key,provider_order_id,checkout_url,status FROM company_listing_payments WHERE listing_id=? AND status IN ('CREATING','PENDING') ORDER BY created_at DESC LIMIT 1", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5), rs.getString(6)}, listingId);
            }
        } else payment = pending.get(0);
        if (payment[4] != null) return new PaymentCheckout(listingId, 5000, "BRL", (String) payment[5], (String) payment[4]);

        try {
            JsonNode order = mercadopago.post().uri("/v1/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Idempotency-Key", payment[2].toString())
                    .body(Map.of("type", "online", "processing_mode", "manual", "capture_mode", "automatic_async", "total_amount", "50.00",
                            "external_reference", payment[1], "description", "Divulgação de vaga de estágio no Trampolim",
                            "items", List.of(Map.of("title", "Divulgação de vaga de estágio", "quantity", 1, "unit_price", "50.00"))))
                    .retrieve().body(JsonNode.class);
            String orderId = order == null ? "" : order.path("id").asText("");
            String checkoutUrl = order == null ? "" : order.path("checkout_url").asText("");
            if (orderId.isBlank() || checkoutUrl.isBlank() || !checkoutUrl.startsWith("https://")) throw new IllegalStateException("Resposta de checkout incompleta.");
            jdbc.update("UPDATE company_listing_payments SET provider_order_id=?,checkout_url=?,provider_status=?,status='PENDING',updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='CREATING'", orderId, checkoutUrl, order.path("status").asText("created"), payment[0]);
            return new PaymentCheckout(listingId, 5000, "BRL", "PENDING", checkoutUrl);
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("internship_checkout_creation_failed listingId={} paymentId={}", listingId, payment[0]);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível iniciar o pagamento. Tente novamente; novas tentativas usam a mesma chave de idempotência.");
        }
    }

    @GetMapping("/api/company/listings/{listingId}/payments")
    public List<PaymentHistory> history(@AuthenticationPrincipal String email, @PathVariable long listingId) {
        long companyId = requireCompanyWriter(email);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM company_listings WHERE id=? AND company_id=?", Integer.class, listingId, companyId) == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.query("SELECT id,amount_cents,provider,status,provider_status,checkout_url,created_at,paid_at FROM company_listing_payments WHERE listing_id=? ORDER BY created_at DESC", (rs, row) -> new PaymentHistory(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant().toString(), rs.getTimestamp(8) == null ? null : rs.getTimestamp(8).toInstant().toString()), listingId);
    }

    @PostMapping("/api/webhooks/mercadopago")
    @Transactional(transactionManager = "empresasTransactionManager")
    @ResponseStatus(HttpStatus.OK)
    public void webhook(@RequestParam(name = "data.id", required = false) String orderId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) WebhookEvent event) {
        if (webhookSecret == null || webhookSecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhook de pagamento ainda não configurado.");
        if (orderId == null || orderId.isBlank() || event == null || !"order".equals(event.type()) || !validSignature(signature, requestId, orderId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Notificação de pagamento inválida.");
        try {
            JsonNode order = mercadopago.get().uri("/v1/orders/{id}", orderId).header("Authorization", "Bearer " + accessToken).retrieve().body(JsonNode.class);
            if (order == null || !orderId.equals(order.path("id").asText())) return;
            List<Object[]> payments = jdbc.query("SELECT id,listing_id,external_reference,amount_cents,status FROM company_listing_payments WHERE provider_order_id=?", (rs, row) -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4), rs.getString(5)}, orderId);
            if (payments.isEmpty()) return;
            Object[] payment = payments.get(0);
            BigDecimal total = decimal(order.path("total_amount").asText("0"));
            BigDecimal paid = decimal(order.path("total_paid_amount").asText("0"));
            if (!payment[2].equals(order.path("external_reference").asText()) || total.compareTo(new BigDecimal("50.00")) != 0) return;
            String providerStatus = order.path("status").asText("unknown");
            if ("processed".equals(providerStatus) && paid.compareTo(new BigDecimal("50.00")) >= 0) {
                jdbc.update("UPDATE company_listing_payments SET status='PAID',provider_status=?,paid_at=COALESCE(paid_at,CURRENT_TIMESTAMP),updated_at=CURRENT_TIMESTAMP WHERE id=?", providerStatus, payment[0]);
                jdbc.update("UPDATE company_listings SET payment_status='PAID',publication_status='PUBLISHED',updated_at=CURRENT_TIMESTAMP WHERE id=? AND is_internship=TRUE", payment[1]);
            } else if ("expired".equalsIgnoreCase(order.path("status_detail").asText()) || "cancelled".equals(providerStatus) || "failed".equals(providerStatus)) {
                String mapped = "expired".equalsIgnoreCase(order.path("status_detail").asText()) ? "EXPIRED" : "CANCELLED";
                jdbc.update("UPDATE company_listing_payments SET status=?,provider_status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'PAID'", mapped, providerStatus, payment[0]);
                jdbc.update("UPDATE company_listings SET payment_status=?,publication_status='AWAITING_PAYMENT',updated_at=CURRENT_TIMESTAMP WHERE id=? AND is_internship=TRUE AND payment_status<>'PAID'", mapped, payment[1]);
            } else jdbc.update("UPDATE company_listing_payments SET provider_status=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status<>'PAID'", providerStatus, payment[0]);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível confirmar o status da order no provedor.");
        }
    }

    private PaymentCheckout latestCheckout(long listingId, int amount) {
        return jdbc.query("SELECT status,checkout_url FROM company_listing_payments WHERE listing_id=? ORDER BY created_at DESC LIMIT 1", (rs, row) -> new PaymentCheckout(listingId, amount, "BRL", rs.getString(1), rs.getString(2)), listingId).stream().findFirst().orElse(new PaymentCheckout(listingId, amount, "BRL", "PAID", null));
    }
    private long requireCompanyWriter(String email) {
        List<Long> owned = jdbc.query("SELECT c.id FROM companies c JOIN company_users u ON u.id=c.owner_user_id WHERE LOWER(u.email)=LOWER(?) AND c.verification_status='APPROVED'", (rs, row) -> rs.getLong(1), email);
        if (!owned.isEmpty()) return owned.get(0);
        List<Object[]> memberships = jdbc.query("SELECT m.company_id,m.role FROM company_members m JOIN companies c ON c.id=m.company_id JOIN company_users u ON u.id=m.user_id WHERE LOWER(u.email)=LOWER(?) AND c.verification_status='APPROVED'", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2)}, email);
        if (memberships.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta área é exclusiva para contas empresariais.");
        Object[] membership = memberships.get(0);
        if (!List.of("OWNER", "RECRUITER").contains(membership[1])) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta ação.");
        return (long) membership[0];
    }
    private boolean validSignature(String signature, String requestId, String dataId) {
        if (signature == null) return false;
        String ts = null, v1 = null;
        for (String piece : signature.split(",")) { String[] pair=piece.trim().split("=",2); if (pair.length==2) { if ("ts".equals(pair[0])) ts=pair[1]; if ("v1".equals(pair[0])) v1=pair[1]; } }
        if (ts == null || v1 == null) return false;
        try {
            String manifest = "id:" + dataId + ";" + (requestId == null || requestId.isBlank() ? "" : "request-id:" + requestId + ";") + "ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(v1);
            return MessageDigest.isEqual(computed, supplied);
        } catch (Exception exception) { return false; }
    }
    private BigDecimal decimal(String value) { try { return new BigDecimal(value); } catch (NumberFormatException exception) { return BigDecimal.ZERO; } }
}
