package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.tool.ProtostuffSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by MyPC on 2015/10/6.
 */
public class RpcEncoder extends MessageToByteEncoder {

    private String serializeType;
    private Class<?> genericClass;

    public RpcEncoder(String serializeType, Class<?> genericClass) {
//        this.serializeType = serializeType;
        this.serializeType = "protostuff";
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
//        if (genericClass.equals(RpcResponse.class)) {
//            // TODO WHY seperate the request id ???
////            RpcResponse response = (RpcResponse) o;
////            String requestId = response.getRequestId();
////            byte[] requestIdBytes = requestId.getBytes();
////            response.setRequestId("");
//
//            byte[] body = new byte[0];
//            if (serializeType.equals("protostuff")) {
//                // TODO cache serialize result
//                body = ProtostuffSerializer.serialize(o);
//            } else if (serializeType.equals("kryo")) {
//                // TODO
//            }
//
////            int totalLen = 4 + requestIdBytes.length + body.length;
////            byteBuf.writeInt(totalLen);
////            byteBuf.writeInt(requestIdBytes.length);
////            byteBuf.writeBytes(requestIdBytes);
//            byteBuf.writeInt(body.length);
//            byteBuf.writeBytes(body);
//
//        } else if (genericClass.equals(RpcRequest.class)) {

        if (genericClass.isInstance(o)) {
            byte[] body = new byte[0];
            if (serializeType.equals("protostuff")) {
                // TODO cache serialize result
                body = ProtostuffSerializer.serialize(o);
            } else if (serializeType.equals("kryo")) {
                // TODO
            }
            byteBuf.writeInt(body.length);
            byteBuf.writeBytes(body);
        }

//        System.out.println("Encode: " + genericClass.getName());
    }
}
