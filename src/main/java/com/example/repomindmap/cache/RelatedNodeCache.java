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

    private static final Map<String, Map<String, MethodNode>> cache = new HashMap<>();

    private static final Map<String, Map<String, List<MethodNode>>> cache2 = new HashMap<>();
    private static final Map<String, Map<String, String>> classCache = new HashMap<>();

    private static final Map<String, Map<String, String>> methodCacheNames = new HashMap<>();

    private static final Logger logger = Logger.getLogger(RelatedNodeCache.class.getName());
    private static final long TTL = 3600000;

    public Optional<MethodNode> getMethodData(String repoUrl, String methodKey) {
        Map<String, MethodNode> repoCache = cache.get(repoUrl);
        return repoCache != null ? Optional.ofNullable(repoCache.get(methodKey)) : Optional.empty();
    }

    public Optional<List<MethodNode>> getFetchMethodList(String repoUrl, String methodKey) {
        Map<String, List<MethodNode>> repoCache = cache2.get(repoUrl);
        return repoCache != null ? Optional.ofNullable(repoCache.get(methodKey)) : Optional.empty();
    }

    public void put(String repoUrl, String methodKey, MethodNode methodNode) {
        cache.computeIfAbsent(repoUrl, k -> new HashMap<>()).put(methodKey, methodNode);
    }

    public void put2(String repoUrl, String methodKey, List<MethodNode> methodNodes) {
        cache2.computeIfAbsent(repoUrl, k -> new HashMap<>()).put(methodKey, methodNodes);
    }



    public Optional<String> getMethodsForClass(String repoUrl, String methodKey) {
        Map<String, String> repoCache = classCache.get(repoUrl);
        return repoCache != null ? Optional.ofNullable(repoCache.get(methodKey)) : Optional.empty();
    }
    public Optional<String> getMethodCacheNames(String repoUrl, String methodKey) {

        Map<String, String> repoCache = methodCacheNames.get(repoUrl);
        return repoCache != null ? Optional.ofNullable(repoCache.get(methodKey)) : Optional.empty();
    }

    public void putMethodCacheClassNames(String repoUrl, String methodKey, String className) {
        methodCacheNames.computeIfAbsent(repoUrl, k -> new HashMap<>()).put(methodKey, className);
    }

    public void putClassCache(String repoUrl, String methodKey, String className) {
        classCache.computeIfAbsent(repoUrl, k -> new HashMap<>()).put(methodKey, className);
    }

    public Map<String, MethodNode> getAll(String repoUrl) {
        return cache.getOrDefault(repoUrl, new HashMap<>());
    }

    public Map<String, List<MethodNode>> getAll2(String repoUrl) {
        return cache2.getOrDefault(repoUrl, new HashMap<>());
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
