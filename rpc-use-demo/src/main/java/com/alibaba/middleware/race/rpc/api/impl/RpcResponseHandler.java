package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.async.ResponseCallbackListener;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.ConcurrentSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by MyPC on 2015/10/6.
 */
public class RpcResponseHandler extends ChannelInboundHandlerAdapter {

    private Map<String, ResultFuture<Object>> futureMap;

    public RpcResponseHandler(Map<String, ResultFuture<Object>> futureMap) {
        this.futureMap = futureMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcResponse response = (RpcResponse) msg;
        String requestId = response.getRequestId();
        futureMap.remove(requestId).setResult(response);

//        System.out.println("Response Handler-");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
