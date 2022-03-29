package com.xiaobo.simple.river.rule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javafx.util.Pair;
import lombok.Data;

/**
 * @author John Bi
 */
@Data
public class Rule {
    private String source;
    private String sourceDatabase;
    private String sink;
    private String sinkIndex;
    private Integer threadNumber;
    private List<RuleCollection> rules;

    public URI parseSink() throws URISyntaxException {
        return new URI(this.sink);
    }

    public Pair<String, String> sinkUserInfo(URI uri) {
        String[] split = uri.getUserInfo().split(":");
        if (split.length != 2) {
            return null;
        }
        return new Pair<>(split[0], split[1]);
    }
}
