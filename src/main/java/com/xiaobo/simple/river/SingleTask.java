package com.xiaobo.simple.river;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.mongodb.reactivestreams.client.MongoClient;
import com.xiaobo.simple.river.rule.Rule;
import lombok.AllArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@CommonsLog
@AllArgsConstructor
public class SingleTask implements Callable<Disposable> {

    /**
     * Using this to specify the task order, build for multi-thread query
     */
    private final int index;
    /**
     * Single query batch size, so each thread will query
     *
     * <code>offset: Index * ChunkSize, Limit: ChunkSize</code>
     */
    private final int chunkSize;
    /**
     * The mongo collection name
     */
    private final String mongoCollection;
    /**
     * The MongoDB client
     */
    private final MongoClient sourceClient;
    /**
     * Convert the MongoDB document to the Elasticsearch upload map
     */
    private final Function<Document, Map<String, Object>> mapper;
    /**
     * The Elasticsearch async client
     */
    private final ElasticsearchAsyncClient sinkClient;

    private final Rule rule;
    private final CountDownLatch latch;

    /**
     * Each thread will query the MongoDB collection, and upload to Elasticsearch
     *
     * @return the current task number
     * @throws Exception empty
     */
    @Override
    public Disposable call() throws Exception {

        // client is pool: http://mongodb.github.io/mongo-java-driver/3.5/driver/getting-started/quick-start/
        return Mono.just(sourceClient.getDatabase(rule.getSourceDatabase()).getCollection(mongoCollection))
                .flatMapMany(col -> col.find().skip(index * chunkSize).limit(chunkSize))
                .map(mapper)
                .map(data -> BulkOperation.of(builder -> builder.index(b -> b.index(rule.getSinkIndex()).id(data.get("id").toString()).document(data))))
                // is best number? maybe 50,000 is better
                .buffer(3000)
                // https://www.vinsguru.com/reactor-schedulers-publishon-vs-subscribeon/
                .flatMap(list ->
                        Mono.fromFuture(sinkClient.bulk(bulk -> bulk.index(rule.getSinkIndex()).operations(list)))
                )
                .doOnNext(res -> log.info(Thread.currentThread().getName() + ": bulk upload error? " + res.errors()))
                .doOnError(e -> log.error("reactor error", e))
                .doFinally(sign -> {
                    latch.countDown();
                    log.info(sign + "=================>" + latch.getCount());
                })
                .subscribe();
    }
}