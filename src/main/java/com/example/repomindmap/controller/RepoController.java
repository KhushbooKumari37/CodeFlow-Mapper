package com.example.repomindmap.controller;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.cache.RepoCache;
import com.example.repomindmap.request.RepoNodeRequest;
import com.example.repomindmap.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class RepoController {

    @Autowired
    private RepoService repoService;

    @Autowired
    private RepoCache repoCache;

    @PostMapping("/generate-mindmap")
    public ResponseEntity<Map<String, ClassOrInterfaceNode>> generateMindMap(@RequestBody Map<String, String> request) {
        String repoUrl = request.get("repoUrl");

        if (repoUrl == null || repoUrl.isEmpty()) {
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.BAD_REQUEST);
        }

        try {
            Map<String, ClassOrInterfaceNode> mindMapJson = repoService.generateMindMap(repoUrl);
            return new ResponseEntity<>(mindMapJson, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/repo/generateMap")
    public ResponseEntity<Map<String, ClassOrInterfaceNode>> getRepoData(@RequestBody Map<String, String> request) {
        try {
            String repoUrl = request.get("repoUrl");
            String decodedRepoUrl = java.net.URLDecoder.decode(repoUrl, StandardCharsets.UTF_8.name());
            return repoCache.getRepoData(decodedRepoUrl)
                    .map(repoData -> new ResponseEntity<>(repoData.getMindMap(), HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/repo/contains")
    public ResponseEntity<String> containsRepoData(@RequestParam String repoName) {
        boolean exists = repoCache.containsRepoData(repoName);
        return exists
                ? new ResponseEntity<>("Repository data exists.", HttpStatus.OK)
                : new ResponseEntity<>("Repository data does not exist.", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/repo/cacheSize")
    public ResponseEntity<String> getCacheSize() {
        int cacheSize = repoCache.getCacheSize();
        return new ResponseEntity<>("Cache size: " + cacheSize, HttpStatus.OK);
    }

    @PostMapping("/repo/clearCache")
    public ResponseEntity<String> clearCache() {
        repoCache.clearCache();
        return new ResponseEntity<>("Cache cleared.", HttpStatus.OK);
    }

    @PostMapping("/repo/getAll")
    public ResponseEntity<Set<String>> getAllNodeKeys(@RequestBody String repoUrl) {
        return new ResponseEntity<>(repoCache.getRepoData(repoUrl).get().getMindMap().keySet(), HttpStatus.OK);
    }

    @PostMapping("/repo/getNodeInfo")
    public ResponseEntity<ClassOrInterfaceNode> getNodeInfo(@RequestBody RepoNodeRequest request) {
        try {
            ClassOrInterfaceNode node = repoCache.getRepoData(request.getRepoUrl())
                    .orElseThrow(() -> new NoSuchElementException("Repository data not found"))
                    .getMindMap()
                    .get(request.getNodeKey());

            return new ResponseEntity<>(node, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}