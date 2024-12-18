package com.example.repomindmap.cache;

import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.util.GitCloneUtil;
import com.example.repomindmap.RepoMindMapGenerator;
import com.example.repomindmap.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class RepoCache {

    private static final Map<String, RepoData> cache = new HashMap<>();
    private static final Logger logger = Logger.getLogger(RepoCache.class.getName());

    private final RepoService repoService;
    private final GitCloneUtil gitCloneUtil;
    private static final long TTL = 3600000;

    @Autowired
    public RepoCache(RepoService repoService, GitCloneUtil gitCloneUtil) {
        this.repoService = repoService;
        this.gitCloneUtil = gitCloneUtil;
    }

    public Map<String, ClassOrInterfaceNode> generateMindMap(String repoUrl) {
        File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
        return RepoMindMapGenerator.generateMindMap(clonedRepo);
    }
    public synchronized Optional<RepoData> getRepoData(String repoUrl) {
        RepoData data = cache.get(repoUrl);

        if (data == null || isExpired(data)) {
            logger.warning("Cache miss or expired for repository: " + repoUrl);
            Map<String, ClassOrInterfaceNode> mindMap = generateMindMap(repoUrl);
            data = new RepoData(mindMap);
            cache.put(repoUrl, data);
        }
        return Optional.ofNullable(data);
    }

    public synchronized boolean containsRepoData(String repoName) {
        RepoData data = cache.get(repoName);
        return data != null && !isExpired(data);
    }

    public synchronized void clearCache() {
        logger.info("Clearing the entire cache");
        cache.clear();
    }

    public synchronized int getCacheSize() {
        return cache.size();
    }

    private static boolean isExpired(RepoData data) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - data.getLastAccessedTime()) > TTL;
    }

    public static class RepoData {
        private final Map<String, ClassOrInterfaceNode> mindMap;
        private final long lastAccessedTime;

        public RepoData(Map<String, ClassOrInterfaceNode> mindMap) {
            this.mindMap = new HashMap<>(mindMap);
            this.lastAccessedTime = System.currentTimeMillis();
        }

        public Map<String, ClassOrInterfaceNode> getMindMap() {
            return mindMap;
        }

        public long getLastAccessedTime() {
            return lastAccessedTime;
        }

        @Override
        public String toString() {
            return "RepoData{" +
                    "mindMap=" + mindMap +
                    ", lastAccessedTime=" + lastAccessedTime +
                    '}';
        }
    }
}
