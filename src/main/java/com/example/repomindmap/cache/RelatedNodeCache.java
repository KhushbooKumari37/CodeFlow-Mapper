package com.example.repomindmap.cache;

import com.example.repomindmap.model.MethodNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class RelatedNodeCache {

    private static final Map<String, MethodData> cache = new HashMap<>();
    private static final Logger logger = Logger.getLogger(RelatedNodeCache.class.getName());
    private static final long TTL = 3600000;

    public synchronized Optional<MethodNode> getMethodData(String methodKey, MethodGenerator generator) {
        MethodData data = cache.get(methodKey);
        if (data == null || isExpired(data)) {
            logger.warning("Cache miss or expired for method: " + methodKey);
            MethodNode methodNode = generator.generate(methodKey);
            if (methodNode != null) {
                data = new MethodData(methodNode);
                cache.put(methodKey, data);
            }
        }

        return Optional.ofNullable(data).map(MethodData::getMethodNode);
    }

    private static boolean isExpired(MethodData data) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - data.getLastAccessedTime()) > TTL;
    }
    public static class MethodData {
        private final MethodNode methodNode;
        private final long lastAccessedTime;

        public MethodData(MethodNode methodNode) {
            this.methodNode = methodNode;
            this.lastAccessedTime = System.currentTimeMillis();
        }

        public MethodNode getMethodNode() {
            return methodNode;
        }

        public long getLastAccessedTime() {
            return lastAccessedTime;
        }

        @Override
        public String toString() {
            return "MethodData{" +
                    "methodNode=" + methodNode +
                    ", lastAccessedTime=" + lastAccessedTime +
                    '}';
        }
    }
    @FunctionalInterface
    public interface MethodGenerator {
        MethodNode generate(String methodKey);
    }
}