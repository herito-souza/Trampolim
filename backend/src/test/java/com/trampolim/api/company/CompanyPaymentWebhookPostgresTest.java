package com.trampolim.api.company;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Real disposable PostgreSQL schema; all payment-provider responses are local synthetic fixtures. */
class CompanyPaymentWebhookPostgresTest {
 static JdbcTemplate admin,jdbc;static String schema;static long company;static final String SECRET="synthetic-qa-webhook-secret-never-production";
 @BeforeAll static void setup(){
  String url=System.getenv().getOrDefault("EMPRESAS_DB_URL","jdbc:postgresql://localhost:5433/trampolim_empresas_db"),user=System.getenv().getOrDefault("EMPRESAS_DB_USERNAME","trampolim_empresas"),password=System.getenv().getOrDefault("EMPRESAS_DB_PASSWORD","dev_empresas_only");
  admin=new JdbcTemplate(new DriverManagerDataSource(url,user,password));schema="payment_qa_"+UUID.randomUUID().toString().replace("-","");admin.execute("CREATE SCHEMA "+schema);
  var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
  Flyway.configure().dataSource(source).locations("classpath:db/migration-empresas").load().migrate();jdbc=new JdbcTemplate(source);
  long owner=jdbc.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status) VALUES ('payment-qa@example.test','synthetic','QA fixture',TRUE,'ACTIVE') RETURNING id",Long.class);
  company=jdbc.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Synthetic QA','Synthetic QA','11222333000181','000','Test','Test','Irecê','BA','APPROVED') RETURNING id",Long.class,owner);
 }
 @AfterAll static void cleanup(){if(admin!=null&&schema!=null)admin.execute("DROP SCHEMA "+schema+" CASCADE");}
 long listing(String status){
  long id=jdbc.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,publication_status,is_internship,payment_status,price_cents) VALUES (?,'job','Synthetic QA internship','Test','Fixture','Fixture','Irecê/BA','Presencial','https://example.test','qa@example.test',?,TRUE,'PENDING',5000) RETURNING id",Long.class,company,status);
  jdbc.update("INSERT INTO company_listing_payments(listing_id,company_id,external_reference,idempotency_key,amount_cents,status,provider_order_id) VALUES (?,?,?,?,5000,'PENDING',?)",id,company,"qa-"+id,UUID.randomUUID(),"order-"+id);return id;
 }
 void notify(long id,String status,String amount,boolean valid)throws Exception{
  var builder=RestClient.builder().baseUrl("https://provider.invalid");var server=MockRestServiceServer.bindTo(builder).build();var controller=new CompanyPaymentController(jdbc);
  ReflectionTestUtils.setField(controller,"mercadopago",builder.build());ReflectionTestUtils.setField(controller,"accessToken","synthetic-qa");ReflectionTestUtils.setField(controller,"webhookSecret",SECRET);
  String order="order-"+id,ts="12345",request="synthetic-request";var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  String signature="ts="+ts+",v1="+HexFormat.of().formatHex(mac.doFinal(("id:"+order+";request-id:"+request+";ts:"+ts+";").getBytes(StandardCharsets.UTF_8)));
  if(!valid){var error=assertThrows(ResponseStatusException.class,()->controller.webhook(order,"ts=12345,v1=00",request,new CompanyPaymentController.WebhookEvent("order",null)));assertEquals(401,error.getStatusCode().value());return;}
  server.expect(requestTo("https://provider.invalid/v1/orders/"+order)).andRespond(withSuccess("{\"id\":\""+order+"\",\"external_reference\":\"qa-"+id+"\",\"total_amount\":\""+amount+"\",\"total_paid_amount\":\""+amount+"\",\"status\":\""+status+"\"}",MediaType.APPLICATION_JSON));
  controller.webhook(order,signature,request,new CompanyPaymentController.WebhookEvent("order",null));server.verify();
 }
 String publication(long id){return jdbc.queryForObject("SELECT publication_status FROM company_listings WHERE id=?",String.class,id);}
 @Test void validPaymentPublishesOnlyPendingAndNeverReopensClosedOrPaused()throws Exception{
  long id=listing("AWAITING_PAYMENT");notify(id,"processed","50.00",true);assertEquals("PUBLISHED",publication(id));
  for(String state:List.of("CLOSED","PAUSED")){jdbc.update("UPDATE company_listings SET publication_status=? WHERE id=?",state,id);notify(id,"processed","50.00",true);assertEquals(state,publication(id));}
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM company_listing_payments WHERE listing_id=?",Integer.class,id));
  assertEquals("PAID",jdbc.queryForObject("SELECT payment_status FROM company_listings WHERE id=?",String.class,id));
 }
 @Test void expiryAndSuspendedCompanyPreventAutomaticPublication()throws Exception{
  long id=listing("AWAITING_PAYMENT");jdbc.update("UPDATE company_listings SET expires_at=CURRENT_DATE-1 WHERE id=?",id);notify(id,"processed","50.00",true);assertEquals("AWAITING_PAYMENT",publication(id));
  long other=listing("AWAITING_PAYMENT");jdbc.update("UPDATE companies SET verification_status='PENDING' WHERE id=?",company);
  try{notify(other,"processed","50.00",true);assertEquals("AWAITING_PAYMENT",publication(other));}finally{jdbc.update("UPDATE companies SET verification_status='APPROVED' WHERE id=?",company);}
 }
 @Test void invalidSignatureAndWrongAmountCannotConfirmPayment()throws Exception{
  long id=listing("AWAITING_PAYMENT");notify(id,"processed","50.00",false);notify(id,"processed","49.00",true);
  assertEquals("PENDING",jdbc.queryForObject("SELECT payment_status FROM company_listings WHERE id=?",String.class,id));assertEquals("AWAITING_PAYMENT",publication(id));
 }
 @Test void failedPaymentDoesNotResetClosedLifecycle()throws Exception{
  long id=listing("CLOSED");notify(id,"failed","50.00",true);assertEquals("CLOSED",publication(id));
 }
}
