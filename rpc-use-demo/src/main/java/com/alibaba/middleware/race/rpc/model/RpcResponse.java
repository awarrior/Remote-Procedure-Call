package com.alibaba.middleware.race.rpc.model;

import java.io.Serializable;

/**
 * Created by MyPC on 2015/10/5.
 */
public class RpcResponse implements Serializable {
    private String requestId;
    private String errorMsg;
    private Object appResponse;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getAppResponse() {
        return appResponse;
    }

    public void setAppResponse(Object appResponse) {
        this.appResponse = appResponse;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public boolean isError() {
        return this.errorMsg != null;
    }
}
