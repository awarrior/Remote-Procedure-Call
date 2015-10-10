package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.async.ResponseCallbackListener;
import com.alibaba.middleware.race.rpc.model.RpcResponse;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by MyPC on 2015/10/7.
 */
public class ResultFuture<T> implements Future<T> {

    // control produce and consume
    private Semaphore semaphore = new Semaphore(0);
    private AtomicBoolean atomicBoolean = new AtomicBoolean(false);
    // response result
    private T result;
    // call back listener
    private ResponseCallbackListener listener = null;

    public void setListener(ResponseCallbackListener listener) {
        this.listener = listener;
    }

    /**
     * set result response for RpcResponseHandler
     */
    public void setResult(T result) {
        if (!atomicBoolean.getAndSet(true)) {
            this.result = result;
            semaphore.release(1);

            // call back
            if (listener != null) {
                if (result.getClass() == RpcResponse.class) {
                    RpcResponse response = (RpcResponse) result;
                    if (response.isError()) {
                        listener.onException((Exception) response.getAppResponse());
                    } else {
                        listener.onResponse(response.getAppResponse());
                    }
                }
            }
        }
    }

    /**
     * get object within timeout
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        T obj = null;
        if (semaphore.tryAcquire(timeout, unit)) {
            obj = result;
            atomicBoolean.set(false);
        }
        return obj;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return null;
    }
}
