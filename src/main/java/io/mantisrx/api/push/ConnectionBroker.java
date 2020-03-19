package io.mantisrx.api.push;

import com.google.inject.name.Named;
import io.mantisrx.api.services.JobDiscoveryService;
import io.mantisrx.api.util.JacksonObjectMapper;
import io.mantisrx.client.MantisClient;
import io.mantisrx.client.SinkConnectionFunc;
import io.mantisrx.client.SseSinkConnectionFunction;
import io.mantisrx.common.MantisServerSentEvent;
import io.mantisrx.runtime.parameter.SinkParameters;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConnectionBroker {

    private final MantisClient mantisClient;
    private final JobDiscoveryService jobDiscoveryService;
    private final Scheduler scheduler;

    private final Map<PushConnectionDetails, Observable<String>> connectionCache = new ConcurrentHashMap<>();

    public ConnectionBroker(MantisClient mantisClient, @Named("io-scheduler") Scheduler scheduler) {
        this.mantisClient = mantisClient;
        this.jobDiscoveryService = JobDiscoveryService.getInstance(mantisClient, scheduler);
        this.scheduler = scheduler;
    }

    public Observable<String> connect(PushConnectionDetails details) {

        if (!connectionCache.containsKey(details)) {
            switch (details.type) {
                case CONNECT_BY_NAME:
                    connectionCache.put(details,
                            getResults(false, this.mantisClient, details.target, new SinkParameters.Builder().build())
                                    .flatMap(m -> m)
                                    .map(m -> m.getEventAsString())
                                    .subscribeOn(scheduler)
                                    .doOnUnsubscribe(() -> {
                                        log.info("Purging {} from cache.", details);
                                        connectionCache.remove(details);
                                    })
                                    .share());
                    break;
                case CONNECT_BY_ID:
                    connectionCache.put(details,
                            getResults(true, this.mantisClient, details.target, new SinkParameters.Builder().build())
                                    .flatMap(m -> m)
                                    .map(m -> m.getEventAsString())
                                    .subscribeOn(scheduler)
                                    .doOnCompleted(() -> {
                                        log.info("Purging {} from cache.", details);
                                        connectionCache.remove(details);
                                    })
                                    .share());
                    break;
                case JOB_STATUS:
                    connectionCache.put(details,
                            mantisClient
                                    .getJobStatusObservable(details.target)
                                    .subscribeOn(scheduler)
                                    .doOnCompleted(() -> {
                                        log.info("Purging {} from cache.", details);
                                        connectionCache.remove(details);
                                    })
                                    .share());
                    break;
                case JOB_SCHEDULING_INFO:
                    connectionCache.put(details,
                            mantisClient.getSchedulingChanges(details.target)
                                    .subscribeOn(scheduler)
                                    .map(changes -> Try.of(() -> JacksonObjectMapper.getInstance().writeValueAsString(changes)).getOrElse("Error"))
                                    .doOnCompleted(() -> {
                                        log.info("Purging {} from cache.", details);
                                        connectionCache.remove(details);
                                    })
                                    .share());
                    break;
                case JOB_CLUSTER_DISCOVERY:
                    connectionCache.put(details,
                            // TODO: We may not want to cache some of these, or maybe replay a few messages given that they follow a cold then hot format.
                            jobDiscoveryService.jobDiscoveryInfoStream(jobDiscoveryService.key(JobDiscoveryService.LookupType.JOB_CLUSTER, details.target))
                                    .subscribeOn(scheduler)
                                    .map(jdi ->Try.of(() -> JacksonObjectMapper.getInstance().writeValueAsString(jdi)).getOrElse("Error"))
                                    .doOnCompleted(() -> {
                                        log.info("Purging {} from cache.", details);
                                        connectionCache.remove(details);
                                    })
                                    .share());
                    break;
            }
            log.info("Caching connection for: {}", details);
        }
        return connectionCache.get(details);
    }


    private static SinkConnectionFunc<MantisServerSentEvent> getSseConnFunc(final String target, SinkParameters sinkParameters) {
        return new SseSinkConnectionFunction(true,
                t -> log.warn("Reconnecting to sink of job " + target + " after error: " + t.getMessage()),
                sinkParameters);
    }

    private static Observable<Observable<MantisServerSentEvent>> getResults(boolean isJobId, MantisClient mantisClient,
                                                                            final String target, SinkParameters sinkParameters) {
        final AtomicBoolean hasError = new AtomicBoolean();
        return  isJobId ?
                mantisClient.getSinkClientByJobId(target, getSseConnFunc(target, sinkParameters), null).getResults() :
                mantisClient.getSinkClientByJobName(target, getSseConnFunc(target, sinkParameters), null)
                        .switchMap(serverSentEventSinkClient -> {
                            if (serverSentEventSinkClient.hasError()) {
                                hasError.set(true);
                                return Observable.error(new Exception(serverSentEventSinkClient.getError()));
                            }
                            return serverSentEventSinkClient.getResults();
                        })
                        .takeWhile(o -> !hasError.get());
    }
}