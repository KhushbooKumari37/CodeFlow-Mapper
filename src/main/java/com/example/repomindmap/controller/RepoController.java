package com.example.repomindmap.controller;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.cache.RepoCache;
import com.example.repomindmap.model.MethodNode;
import com.example.repomindmap.request.RepoNodeRequest;
import com.example.repomindmap.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public ResponseEntity<Map<String, List<String>>> getAllNodeKeys(@RequestBody Map<String, String> request) {
        Optional<RepoCache.RepoData> repoDataOptional = repoCache.getRepoData(request.get("repoUrl"));
        if (repoDataOptional.isPresent()) {
            Map<String, List<String>> groupedByPackage = repoDataOptional.get().getMindMap().keySet()
                    .stream()
                    .collect(Collectors.groupingBy(
                            key -> key.substring(0, key.lastIndexOf('.')),
                            Collectors.mapping(
                                    key -> key.substring(key.lastIndexOf('.') + 1),
                                    Collectors.toList()
                            )
                    ));
            return new ResponseEntity<>(groupedByPackage, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/repo/getNodeInfo")
    public ResponseEntity<ClassOrInterfaceNode> getNodeInfo (@RequestBody RepoNodeRequest request) {
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


    @PostMapping("/repo/getParentInterfaces")
    public ResponseEntity<List<ClassOrInterfaceNode>> getParentInterfaces(@RequestBody RepoNodeRequest request) {
        try {
            Map<String, ClassOrInterfaceNode> mindMap = repoCache.getRepoData(request.getRepoUrl())
                    .orElseThrow(() -> new NoSuchElementException("Repository data not found"))
                    .getMindMap();

            ClassOrInterfaceNode targetNode = mindMap.get(request.getNodeKey());
            if (targetNode == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            List<ClassOrInterfaceNode> parentInterfaces = new ArrayList<>();
            collectParentInterfaces(targetNode, parentInterfaces, mindMap);

            return new ResponseEntity<>(parentInterfaces, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/repo/getParentClass")
    public ResponseEntity<ClassOrInterfaceNode> getParentClass(@RequestBody Map<String, String> requestBody) {
        try {
            String repoUrl = requestBody.get("repoUrl");
            String nodeKey = requestBody.get("nodeKey");

            Map<String, ClassOrInterfaceNode> mindMap = repoCache.getRepoData(repoUrl)
                    .orElseThrow(() -> new NoSuchElementException("Repository data not found"))
                    .getMindMap();

            ClassOrInterfaceNode currentNode = mindMap.get(nodeKey);
            if (currentNode == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            List<ClassOrInterfaceNode> parentClassNodes = currentNode.getExtendsNode();
            if (parentClassNodes != null && !parentClassNodes.isEmpty()) {
                return new ResponseEntity<>(parentClassNodes.get(0), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void collectParentInterfaces(ClassOrInterfaceNode node, List<ClassOrInterfaceNode> parentInterfaces, Map<String, ClassOrInterfaceNode> mindMap) {
        if (node.getImplementsNode() != null) {
            for (ClassOrInterfaceNode parent : node.getImplementsNode()) {
                parentInterfaces.add(parent);
                ClassOrInterfaceNode parentNode = mindMap.get(parent.getNodeKey());
                if (parentNode != null) {
                    collectParentInterfaces(parentNode, parentInterfaces, mindMap);
                }
            }
        }
    }

}