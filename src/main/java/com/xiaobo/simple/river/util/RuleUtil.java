package com.xiaobo.simple.river.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.xiaobo.simple.river.rule.RulePattern;
import lombok.experimental.UtilityClass;

/**
 * @author John Bi
 */
@UtilityClass
public class RuleUtil {

    public Map<String, RulePattern> toMap(List<RulePattern> rulePatterns) {
        return rulePatterns.stream().collect(Collectors.toMap(RulePattern::getOri, e -> e));
    }

    public Map<String, RulePattern> toMap(RulePattern rulePattern) {
        List<RulePattern> child = rulePattern.getChild();
        if (child != null && !child.isEmpty()) {
            return toMap(child);
        }
        return Collections.emptyMap();
    }
}
