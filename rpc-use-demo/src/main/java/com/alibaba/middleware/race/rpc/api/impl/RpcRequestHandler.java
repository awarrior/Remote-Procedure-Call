package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.context.RpcContext;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by MyPC on 2015/10/6.
 */
public class RpcRequestHandler extends ChannelInboundHandlerAdapter {

    private Object service;

    public RpcRequestHandler(Object service) {
        this.service = service;
    }

    private Object handle(RpcRequest request) throws Throwable {
        // recover context
        RpcContext.props.putAll(request.getContext());

        // TODO cache
        Class<?> serviceClass = service.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] arguments = request.getArguments();

        // TODO compare to cglib - fastmethod
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        return method.invoke(service, arguments);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcRequest request = (RpcRequest) msg;
//        String host = ctx.channel().remoteAddress().toString();
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());

        Object result = null;
        try {
            result = handle(request);
        } catch (Throwable t) {
            result = t.getCause();
            response.setErrorMsg(result.toString());
        } finally {
            response.setAppResponse(result);
        }

        ctx.writeAndFlush(response);

//        System.out.println("Request Handler-");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
