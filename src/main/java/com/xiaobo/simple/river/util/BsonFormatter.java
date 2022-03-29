package com.xiaobo.simple.river.util;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.xiaobo.simple.river.rule.RuleCollection;
import com.xiaobo.simple.river.rule.RulePattern;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.Document;

/**
 * @author John Bi
 */
@Data
@AllArgsConstructor
public class BsonFormatter {

    private Document doc;
    private RuleCollection outline;

    public Map<String, Object> format() {
        Map<String, Object> result = new HashMap<>();
        Map<String, RulePattern> root = outline.toMap();

        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            Optional<Pair<String, RulePattern>> optional = getKey(entry, root);
            optional.ifPresent(pair ->
                    result.put(pair.getKey(), formatValue(entry.getValue(), pair.getValue()))
            );
        }
        // FIXME(bi): if set name same as document field name, how to handle?
        result.putAll(outline.getExtra());
        return result;
    }

    private Object formatValue(Object value, RulePattern parent) {

        if (value instanceof Document) {
            Map<String, RulePattern> childMap = Collections.emptyMap();
            if (parent != null && parent.deepReplace()) {
                childMap = parent.toMap();
            }
            Map<String, Object> documentToMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Document) value).entrySet()) {
                Optional<Pair<String, RulePattern>> optional = getKey(entry, childMap);
                // into the dark side
                optional.ifPresent(pair ->
                        documentToMap.put(pair.getKey(), formatValue(entry.getValue(), pair.getValue()))
                );
            }
            return documentToMap;
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        // other is direct output
        return value;
    }

    private Optional<Pair<String, RulePattern>> getKey(Map.Entry<String, Object> entry, Map<String, RulePattern> rules) {
        String key = entry.getKey();
        RulePattern rulePattern = rules.get(key);
        // exclude the specified key
        if (rulePattern != null && rulePattern.isExclude()) {
            return Optional.empty();
        }
        if (rulePattern != null) {
            key = rulePattern.replace(key);
        }
        return Optional.of(new Pair<>(key, rulePattern));
    }
}
