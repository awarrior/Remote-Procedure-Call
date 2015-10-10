package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.aop.ConsumerHook;
import com.alibaba.middleware.race.rpc.api.RpcConsumer;
import com.alibaba.middleware.race.rpc.async.ResponseCallbackListener;
import com.alibaba.middleware.race.rpc.async.ResponseFuture;
import com.alibaba.middleware.race.rpc.context.RpcContext;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by MyPC on 2015/10/5.
 */
public class RpcConsumerImpl extends RpcConsumer {

    // interface want to use
    private Class<?> interfaceClass;
    // version of service
    private String version;
    // timeout
    private int clientTimeout;
    // hook to service
    private ConsumerHook hook;

    // available channel
    private Channel channel;
    // save <methodName, listener> for async
    private Map<String, ResponseCallbackListener> asyncMethodListeners;
    // save <RequestId, ResultFuture> for interaction with RpcProvider
    private Map<String, ResultFuture<Object>> futureMap;

    // rpc client connection
//    private RpcConsumerConnection connection;
//    private List<RpcConnection> connectionList;


    /**
     * connection
     */
    private void connect(final String host, final int port) {
        final String serializeType = "protostuff";

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new RpcEncoder(serializeType, RpcRequest.class))
                                    .addLast(new RpcDecoder(serializeType, RpcResponse.class))
                                    .addLast(new RpcResponseHandler(futureMap));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            this.channel = channelFuture.channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            group.shutdownGracefully();
        }
    }

    /**
     * initialization
     */
    public RpcConsumerImpl() {
        asyncMethodListeners = new HashMap<>();
        futureMap = new ConcurrentHashMap<>();

        // connect to server
//        String host = System.getProperty("SIP");
        final String host = "127.0.0.1";
        final int port = 8888;
        connect(host, port);

        // create connection list
//        connectionList = new ArrayList<>();
//        int num = Runtime.getRuntime().availableProcessors() / 3 - 2;
    }

    @Override
    public RpcConsumer interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    @Override
    public RpcConsumer version(String version) {
        this.version = version;
        return this;
    }

    @Override
    public RpcConsumer clientTimeout(int clientTimeout) {
        this.clientTimeout = clientTimeout;
        return this;
    }

    @Override
    public RpcConsumer hook(ConsumerHook hook) {
        this.hook = hook;
        return this;
    }

    /**
     * @return proxy instance
     */
    @Override
    public Object instance() {
        return Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, this);
    }

    /**
     * add async method
     */
    @Override
    public void asynCall(String methodName) {
        asynCall(methodName, null);
    }

    /**
     * add async method
     */
    @Override
    public <T extends ResponseCallbackListener> void asynCall(String methodName, T callbackListener) {
        asyncMethodListeners.put(methodName, callbackListener);
    }

    /**
     * cancel async method
     */
    @Override
    public void cancelAsyn(String methodName) {
        asyncMethodListeners.remove(methodName);
    }

    /**
     * dynamic proxy
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // request obj
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setArguments(args);

        // hook - before
        if (hook != null)
            hook.before(request);

        // invoke
        request.setContext(RpcContext.getProps());
        RpcResponse response = send(request);

        // hook - after
        if (hook != null)
            hook.after(request);

        // return
        if (response == null) {
            return null;
        } else if (response.isError()) {
            throw (Exception) response.getAppResponse();
        } else {
            return response.getAppResponse();
        }
    }

    private RpcResponse send(RpcRequest request) {
        String requestId = request.getRequestId();
        String methodName = request.getMethodName();

        // interact with netty handler through ResultFuture
        ResultFuture<Object> future = new ResultFuture<>();
        futureMap.put(requestId, future);

        // send request
        this.channel.writeAndFlush(request);

        if (asyncMethodListeners.containsKey(methodName)) {
            // if is async
            future.setListener(asyncMethodListeners.get(methodName));
            ResponseFuture.futureThreadLocal.set(future);
            return null;
        } else {
            // else is sync
            try {
                return (RpcResponse) future.get(clientTimeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
