package com.xiaobo.simple.river;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.xiaobo.simple.river.rule.Rule;
import com.xiaobo.simple.river.rule.RuleCollection;
import com.xiaobo.simple.river.util.BsonFormatter;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.bson.Document;
import org.elasticsearch.client.RestClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * @author John Bi
 */
@Data
@CommonsLog
public class Process implements IProcess {

    private Rule rule;

    private ElasticsearchTransport transport;
    private MongoClient mongoClient;
    private ElasticsearchAsyncClient client;

    public Process(Rule rule) {
        this.rule = rule;
    }

    private static Long calcChunkSize(Long total, Integer threadNum) {
        if (total == null) {
            return null;
        }
        // like page number calc
        return (total + threadNum - 1) / threadNum;
    }

    @Override
    public void init() throws Exception {
        // mongoDB
        mongoClient = MongoClients.create(rule.getSource());

        // elasticsearch
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        Pair<String, String> userInfo;
        URI link = rule.parseSink();
        if ((userInfo = rule.sinkUserInfo(link)) != null) {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userInfo.getKey(), userInfo.getValue()));
        }

        RestClient restClient = RestClient.builder(new HttpHost(link.getHost(), link.getPort(), link.getScheme()))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    try {
                        return httpClientBuilder
                                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setDefaultCredentialsProvider(credentialsProvider);
                    } catch (Exception e) {
                        log.error("Exception: {}", e);
                        return null;
                    }
                })
                .build();
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchAsyncClient(transport);
    }

    @Override
    public void process() throws Exception {


        ExecutorService pool =
                Executors.newFixedThreadPool(rule.getThreadNumber());


        for (RuleCollection ruleCollection : rule.getRules()) {

            Function<Document, Map<String, Object>> fn = doc -> new BsonFormatter(doc, ruleCollection).format();
            MongoDatabase database = mongoClient.getDatabase(rule.getSourceDatabase());
            MongoCollection<Document> collection = database.getCollection(ruleCollection.getCollection());
            Long chunkSize = Mono.from(collection.countDocuments())
                    .doOnNext(total -> log.info("Total: " + total))
                    .map(total -> calcChunkSize(total, rule.getThreadNumber()))
                    .block();
            log.info("Chunk size: " + chunkSize);

            if (chunkSize == null) {
                continue;
            }

            // using latch to wait all task done.
            CountDownLatch latch = new CountDownLatch(rule.getThreadNumber());
            for (int i = 0; i < rule.getThreadNumber(); i++) {
                SingleTask singleTask = new SingleTask(
                        i, chunkSize.intValue(), ruleCollection.getCollection(),
                        mongoClient, fn, client, rule, latch
                );
                pool.submit(singleTask);
            }
            latch.await();
        }

        log.info("All tasks are done");
        pool.shutdown();
    }

    @Override
    public void destroy() throws Exception {
        try {
            transport.close();
        } catch (IOException e) {
            log.error(e);
        }

        try {
            mongoClient.close();
        } catch (Exception e) {
            log.error(e);
        }
    }
}
