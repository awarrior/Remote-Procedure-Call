package com.alibaba.middleware.race.rpc.tool;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by MyPC on 2015/10/5.
 */
public class ProtostuffSerializer {
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    static {
        // always use sun reflection factory for the serial construct of obj
        // solve issue constructor add element to LIST twice just using ProtostuffIOUtil.mergeFrom
        // * Objenesis is another solution choice.
        System.setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
    }

    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(cls);
            cachedSchema.put(cls, schema);
        }
        return schema;
    }

    /**
     * serialize object to byte[]
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        Schema<T> schema = getSchema(cls);
        LinkedBuffer buf = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buf);
        } finally {
            buf.clear();
        }
    }

    /**
     * deserialize byte[] to object
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        T obj = null;
        try {
            obj = cls.newInstance();
            Schema<T> schema = getSchema(cls);
            ProtostuffIOUtil.mergeFrom(data, obj, schema);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
