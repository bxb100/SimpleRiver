package com.xiaobo.simple.river.rule;

import java.util.List;
import java.util.Map;

import com.xiaobo.simple.river.util.RuleUtil;
import lombok.Data;

/**
 * @author John Bi
 */
@Data
public class RuleCollection {
    private String collection;
    private List<RulePattern> patterns;
    private Map<String, String> extra;

    public Map<String, RulePattern> toMap() {
        return RuleUtil.toMap(this.patterns);
    }
}
