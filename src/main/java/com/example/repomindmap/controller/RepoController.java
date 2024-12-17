package com.example.repomindmap.controller;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.cache.RepoCache;
import com.example.repomindmap.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
    @PostMapping("/repo/addOrUpdate")
    public ResponseEntity<String> addOrUpdateRepoData(@RequestBody Map<String, String> request) {
        return repoCache.addOrUpdateRepoData(request);
    }

    @GetMapping("/repo/{repoUrl}")
    public ResponseEntity<Map<String, ClassOrInterfaceNode>> getRepoData(@PathVariable String repoUrl) {
        try {
            String decodedRepoUrl = java.net.URLDecoder.decode(repoUrl, StandardCharsets.UTF_8.name());
            return repoCache.getRepoData(decodedRepoUrl)
                    .map(repoData -> new ResponseEntity<>(repoData.getMindMap(), HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
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
}