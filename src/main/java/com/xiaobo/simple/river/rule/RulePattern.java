package com.xiaobo.simple.river.rule;

import java.util.List;
import java.util.Map;

import com.xiaobo.simple.river.util.RuleUtil;
import lombok.Data;

@Data
public class RulePattern {
    /**
     * not null
     */
    private String ori;
    private String replace;
    /**
     * <code>exclude</code> always first active, if true, then ignore other logic
     */
    private boolean exclude;
    private List<RulePattern> child;

    public Map<String, RulePattern> toMap() {
        return RuleUtil.toMap(this);
    }

    public String replace(String ori) {
        if (this.replace == null || this.replace.isEmpty()) {
            return ori;
        }
        return this.replace;
    }

    public boolean deepReplace() {
        return this.child != null && !this.child.isEmpty();
    }
}