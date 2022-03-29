* wait for the 8.1.1 release, we will add create index function

```java
JsonpMapper mapper = client._transport().jsonpMapper();
client.indices().exists(b -> b.index(sinkIndex))
                .thenCompose(exists -> {
                    CompletableFuture<DeleteIndexResponse> future =
                            new CompletableFuture<>();
                    if (exists.value()) {
                       
                        future = client.indices().delete(b -> b.index(sinkIndex));
                    }
                    // mapping the data
                    InputStream input = Main.class.getClassLoader().getResourceAsStream("sink.json");
                    // FIXME: need change withJson in 8.1.1 release
                    // https://github.com/elastic/elasticsearch-java/pull/200
                    return future
                            .thenCompose(res -> client.indices().create(fromJson(input, mapper)));
                }).get();
```

sink.json

```json
{
  "mappings": {
    "properties": {
      "url": {
        "type": "text",
        "analyzer": "lowercase_with_stop_words"
      },
      "title": {
        "type": "text"
      },
      "content": {
        "type": "text",
        "analyzer": "simple_html"
      }
    }
  },
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 2
    },
    "analysis": {
      "filter": {
        "stop_words_filter": {
          "type": "stop",
          "stopwords": [
            "http",
            "https",
            "ftp",
            "www"
          ]
        }
      },
      "analyzer": {
        "lowercase_with_stop_words": {
          "type": "custom",
          "tokenizer": "lowercase",
          "filter": [
            "stop_words_filter"
          ]
        },
        "simple_html" : {
          "type": "custom",
          "char_filter" : [
            "html_strip"
          ],
          "tokenizer" : "standard"
        }
      }
    }
  }
}
```