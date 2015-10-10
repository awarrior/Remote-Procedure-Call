package com.alibaba.middleware.race.rpc.api.impl;

import com.alibaba.middleware.race.rpc.tool.ProtostuffSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created by MyPC on 2015/10/6.
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private String serializeType;
    private Class<?> genericClass;

    public RpcDecoder(String serializeType, Class<?> genericClass) {
//        this.serializeType = serializeType;
        this.serializeType = "protostuff";
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 4) {
            return;
        }

        int bodyLen = byteBuf.readInt();
        if (bodyLen < 0) {
            channelHandlerContext.close();
        }

        byteBuf.markReaderIndex();
        if (byteBuf.readableBytes() < bodyLen) {
            byteBuf.resetReaderIndex();
            return;
        }

        byte[] body = new byte[bodyLen];
        byteBuf.readBytes(body);

        // TODO cache to save time
        Object obj = ProtostuffSerializer.deserialize(body, genericClass);
        list.add(obj);

//        System.out.println("Decode: " + genericClass.getName());
    }
}
