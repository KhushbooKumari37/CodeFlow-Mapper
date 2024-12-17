package com.example.repomindmap.cache;

import com.example.repomindmap.RepoMindMapGenerator;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.service.RepoService;
import com.example.repomindmap.util.GitCloneUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

@Component
public class RepoCache {

    private static final ConcurrentMap<String, RepoData> cache = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(RepoCache.class.getName());

    private final RepoService repoService;
    @Autowired
    private GitCloneUtil gitCloneUtil;

    public RepoCache(RepoService repoService) {
        this.repoService = repoService;
    }

    public Map<String, ClassOrInterfaceNode> generateMindMap(String repoUrl) {
        File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
        return RepoMindMapGenerator.generateMindMap(clonedRepo);
    }

    public ResponseEntity<String> addOrUpdateRepoData(Map<String, String> request) {
        String repoUrl = request.get("repoUrl");

        if (repoUrl == null || repoUrl.isEmpty()) {
            return new ResponseEntity<>("Repository URL cannot be null or empty", HttpStatus.BAD_REQUEST);
        }

        try {
            Map<String, ClassOrInterfaceNode> mindMap = generateMindMap(repoUrl);
            RepoData repoData = new RepoData(mindMap);
            cache.put(repoUrl, repoData);

            return new ResponseEntity<>("Repository data added or updated successfully!", HttpStatus.OK);
        } catch (Exception e) {
            logger.severe("Error generating mind map: " + e.getMessage());
            return new ResponseEntity<>("Error generating mind map: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static Optional<RepoData> getRepoData(String repoName) {
        RepoData data = cache.get(repoName);
        if (data == null) {
            logger.warning("Cache miss for repository: " + repoName);
        }
        return Optional.ofNullable(data);
    }

    public static boolean containsRepoData(String repoName) {
        return cache.containsKey(repoName);
    }

    public static void clearCache() {
        logger.info("Clearing the entire cache");
        cache.clear();
    }

    public static int getCacheSize() {
        return cache.size();
    }

    public static class RepoData {
        private final Map<String, ClassOrInterfaceNode> mindMap;

        public RepoData(Map<String, ClassOrInterfaceNode> mindMap) {
            // Use the input map to create an unmodifiable version
            this.mindMap = Collections.unmodifiableMap(new HashMap<>(mindMap));
        }

        public Map<String, ClassOrInterfaceNode> getMindMap() {
            return mindMap;
        }

        @Override
        public String toString() {
            return "RepoData{" +
                    "mindMap=" + mindMap +
                    '}';
        }
    }

}
