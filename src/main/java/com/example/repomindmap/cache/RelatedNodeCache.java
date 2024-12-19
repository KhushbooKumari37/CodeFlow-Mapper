package com.example.repomindmap.cache;

import com.example.repomindmap.model.MethodNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class RelatedNodeCache {

    private static final Map<String, MethodData> cache = new HashMap<>();
    private static final Map<String, List<MethodNode>> cache2 = new HashMap<>();

    private static final Logger logger = Logger.getLogger(RelatedNodeCache.class.getName());
    private static final long TTL = 3600000;

    public synchronized Optional<MethodNode> getMethodData(String methodKey) {
        MethodData data = cache.get(methodKey);
        return Optional.ofNullable(data).map(MethodData::getMethodNode);
    }

    public synchronized Optional<List<MethodNode>> getFetchMethodList(String methodKey) {
        return Optional.ofNullable(cache2.get(methodKey));
    }
    // Add the 'put' method to insert the list of MethodNodes into cache2
    public synchronized void put(String name, List<MethodNode> methodNodes) {
        cache2.put(name, methodNodes);
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
}