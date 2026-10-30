package com.trampolim.api.education;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.security.TrailTokenService;

@RestController
@RequestMapping("/api/capacita/learning/notifications")
@ConditionalOnProperty(name="trampolim.datasource.capacita.enabled",havingValue="true")
public class LearningNotifications {
    private final JdbcTemplate jdbc;private final LearningAccess access;private final TrailTokenService tokens;
    private final ConcurrentHashMap<UUID,Channel> channels=new ConcurrentHashMap<>();
    private final ExecutorService sending=Executors.newVirtualThreadPerTaskExecutor();
    @Value("${capacita.questions.sse-max-clients:500}") private int maximum=500;
    private io.micrometer.core.instrument.Counter pollFailures;
    private io.micrometer.core.instrument.Timer pollTime;
    @Autowired void monitor(io.micrometer.core.instrument.MeterRegistry registry) {
        io.micrometer.core.instrument.Gauge.builder("capacita.sse.connections",channels,Map::size).register(registry);
        io.micrometer.core.instrument.Gauge.builder("capacita.sse.capacity",this,item->item.maximum).register(registry);
        io.micrometer.core.instrument.Gauge.builder("capacita.sse.writing",channels,map->map.values().stream().filter(item->item.writing.get()).count()).register(registry);
        pollFailures=registry.counter("capacita.sse.poll.failures");
        pollTime=registry.timer("capacita.sse.poll.duration");
    }
    private static class Channel {
        final UUID id=UUID.randomUUID();final long user;final int version;final Instant expires;final SseEmitter emitter=new SseEmitter(120000L);final AtomicBoolean writing=new AtomicBoolean();
        volatile long cursor;volatile Instant heartbeat=Instant.EPOCH;
        Channel(long user,int version,Instant expires,long cursor){this.user=user;this.version=version;this.expires=expires;this.cursor=cursor;}
    }
    public LearningNotifications(@Qualifier("capacitaJdbcTemplate") JdbcTemplate jdbc,LearningAccess access,TrailTokenService tokens){this.jdbc=jdbc;this.access=access;this.tokens=tokens;}
    @GetMapping public Map<String,Object> history(@AuthenticationPrincipal String email,@RequestParam(required=false)Long before) {
        var user=access.account(email);var rows=jdbc.queryForList("SELECT id,question_id,kind,read_at,created_at FROM learning_notifications WHERE user_id=? AND id<? ORDER BY id DESC LIMIT 50",user.id(),before==null?Long.MAX_VALUE:before);
        return Map.of("items",rows,"unreadCount",jdbc.queryForObject("SELECT count(*) FROM learning_notifications WHERE user_id=? AND read_at IS NULL",Long.class,user.id()));
    }
    @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@AuthenticationPrincipal String email,@PathVariable long id) {
        if(jdbc.update("UPDATE learning_notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND user_id=?",id,access.account(email).id())==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    @GetMapping(value="/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@AuthenticationPrincipal String email,@RequestHeader("Authorization")String authorization,@RequestParam(defaultValue="0")long after) throws java.io.IOException {
        var user=access.account(email);var decoded=tokens.verify(authorization.replaceFirst("^Bearer ",""),TrailTokenService.Trail.CAPACITA);
        if(decoded==null||after<0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var channel=new Channel(user.id(),decoded.getClaim("ver").asInt(),decoded.getExpiresAtAsInstant(),after);
        synchronized(channels){if(sending.isShutdown())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);if(channels.size()>=maximum||channels.values().stream().filter(item->item.user==user.id()).count()>=2) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de conexões em tempo real atingido.");channels.put(channel.id,channel);}
        channel.emitter.onCompletion(()->channels.remove(channel.id));channel.emitter.onTimeout(()->{channels.remove(channel.id);channel.emitter.complete();});channel.emitter.onError(error->channels.remove(channel.id));
        channel.emitter.send(SseEmitter.event().name("ready").data(Map.of("connected",true)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Accel-Buffering","no").body(channel.emitter);
    }
    @Scheduled(fixedDelayString="${capacita.questions.sse-poll-ms:2000}")
    public void publishPersisted() {
        var snapshot=new ArrayList<>(channels.values());if(snapshot.isEmpty()||sending.isShutdown())return;
        long pollStarted=System.nanoTime();
        try {
            var cursors=new LinkedHashMap<Long,Long>();for(var channel:snapshot)cursors.merge(channel.user,channel.cursor,Math::min);
            var parameters=new ArrayList<Object>();var values=new ArrayList<String>();for(var channel:snapshot){values.add("(?::uuid,?::bigint,?::bigint)");parameters.add(channel.id);parameters.add(channel.user);parameters.add(channel.cursor);}
            String ids=String.join(",",Collections.nCopies(cursors.size(),"?"));
            var accounts=jdbc.queryForList("SELECT id,token_version,account_status,email_verified FROM student_users WHERE id IN ("+ids+")",cursors.keySet().toArray());
            Map<Long,Map<String,Object>> byAccount=new HashMap<>();for(var row:accounts)byAccount.put(((Number)row.get("id")).longValue(),row);
            var notifications=jdbc.queryForList("SELECT c.channel_id,n.* FROM (VALUES "+String.join(",",values)+") AS c(channel_id,user_id,last_id) CROSS JOIN LATERAL (SELECT id,user_id,question_id,kind,created_at FROM learning_notifications WHERE user_id=c.user_id AND id>c.last_id ORDER BY id LIMIT 50) n ORDER BY n.id",parameters.toArray());
            Map<UUID,List<Map<String,Object>>> byChannel=new HashMap<>();for(var row:notifications){UUID channel=(UUID)row.remove("channel_id");byChannel.computeIfAbsent(channel,ignored->new ArrayList<>()).add(row);}
            for(var channel:snapshot){
                var account=byAccount.get(channel.user);
                if(account==null||!"ACTIVE".equals(account.get("account_status"))||!Boolean.TRUE.equals(account.get("email_verified"))||((Number)account.get("token_version")).intValue()!=channel.version||channel.expires.isBefore(Instant.now())){close(channel);continue;}
                var pending=byChannel.getOrDefault(channel.id,List.of()).stream().filter(row->((Number)row.get("id")).longValue()>channel.cursor).toList();
                if((!pending.isEmpty()||channel.heartbeat.plusSeconds(15).isBefore(Instant.now()))&&channel.writing.compareAndSet(false,true)) sending.execute(()->{
                    try {
                        if(!pending.isEmpty()){long last=((Number)pending.get(pending.size()-1).get("id")).longValue();channel.emitter.send(SseEmitter.event().name("notifications").id(Long.toString(last)).data(Map.of("items",pending)));channel.cursor=last;}
                        else channel.emitter.send(SseEmitter.event().name("heartbeat").data(Map.of("alive",true)));
                        channel.heartbeat=Instant.now();
                    } catch(Exception error){close(channel);}finally{channel.writing.set(false);}
                });
            }
        } catch(org.springframework.dao.DataAccessException|RejectedExecutionException error){if(pollFailures!=null)pollFailures.increment();for(var channel:snapshot)close(channel);}
        finally {if(pollTime!=null)pollTime.record(System.nanoTime()-pollStarted,TimeUnit.NANOSECONDS);}
    }
    private void close(Channel channel){channels.remove(channel.id);channel.emitter.complete();}
    @org.springframework.context.event.EventListener(org.springframework.context.event.ContextClosedEvent.class)
    @jakarta.annotation.PreDestroy public void shutdown(){synchronized(channels){sending.shutdownNow();for(var channel:channels.values())close(channel);}}
}
