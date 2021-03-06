package com.zhizus.forest.client.cluster.ha;

import com.zhizus.forest.client.cluster.lb.AbstractLoadBalance;
import com.zhizus.forest.common.ServerInfo;
import com.zhizus.forest.common.codec.Message;
import com.zhizus.forest.common.exception.ForestBizException;
import com.zhizus.forest.transport.NettyClient;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Dempe on 2016/12/7.
 */
public class FailoverStrategy extends AbstractHAStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(FailoverStrategy.class);

    public final static int DEF_TRY_COUNT = 3;
    private int tryNum;

    public FailoverStrategy(GenericKeyedObjectPoolConfig config) {
        super(config);
        this.tryNum = DEF_TRY_COUNT;
    }

    public FailoverStrategy(GenericKeyedObjectPoolConfig config, int tryNum) {
        super(config);
        this.tryNum = tryNum;
    }

    @Override
    public Object call(Message message, AbstractLoadBalance<ServerInfo<NettyClient>> loadBalance) throws Exception {
        List<ServerInfo<NettyClient>> availableServerList = loadBalance.getAvailableServerList();
        for (int i = 0; i < tryNum; i++) {
            try {
                return remoteCall(availableServerList.get(i % availableServerList.size()), message, loadBalance);
            } catch (RuntimeException e) {
                // 对于业务异常，直接抛出
                if (e instanceof ForestBizException) {
                    throw e;
                } else if (i >= tryNum) {
                    throw e;
                }
                LOGGER.warn("FailoverHaStrategy Call false for message:{}, num:{}, exception:{} ", message, i, e.getMessage());
            }
        }
        return null;
    }
}
