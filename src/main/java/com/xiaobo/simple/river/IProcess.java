package com.xiaobo.simple.river;

/**
 * @author John Bi
 */
public interface IProcess {

    void init() throws Exception;

    void process() throws Exception;

    void destroy() throws Exception;
}
