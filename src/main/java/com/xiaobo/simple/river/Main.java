package com.xiaobo.simple.river;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiaobo.simple.river.rule.Rule;
import lombok.extern.apachecommons.CommonsLog;

/**
 * @author John Bi
 */
@CommonsLog
public class Main {

    private static final Rule RULE;

    static {
        ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());
        InputStream ruleYml = Main.class.getClassLoader().getResourceAsStream("rule.yml");
        ymlMapper.findAndRegisterModules();
        try  {
            RULE = ymlMapper.readValue(ruleYml, Rule.class);
        } catch (IOException e) {
            throw new Error(e);
        }

    }


    public static void main(String[] args) throws Exception {

        Process process = new Process(RULE);

        try {
            process.init();
            process.process();
            log.info("process finished");
        } finally {
            process.destroy();
        }
    }
}
