package com.example.repomindmap.controller;

import com.example.repomindmap.cache.RelatedNodeCache;
import com.example.repomindmap.cache.RepoCache;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.example.repomindmap.request.RepoNodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(value = "*")
public class RepoController {
    @Autowired
    private RepoCache repoCache;
    @Autowired
    private RelatedNodeCache relatedNodeCache;

    @PostMapping("/getMethodNode")
    public ResponseEntity<MethodNode> getMethodNode(@RequestBody Map<String, String> request) {
        String repoUrl = request.get("repoUrl");
        String methodKey = request.get("methodKey");

        if (repoUrl == null || methodKey == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<MethodNode> methodNode = relatedNodeCache.getMethodData(repoUrl, methodKey);
        return methodNode
                .map(node -> new ResponseEntity<>(node, HttpStatus.OK))
                .orElse(new ResponseEntity<>(null,HttpStatus.OK));
    }


    @PostMapping("/repo/generateMap")
    public ResponseEntity<Map<String, ClassOrInterfaceNode>> getRepoData(@RequestBody Map<String, String> request) {
        try {
            String repoUrl = request.get("repoUrl");
            String decodedRepoUrl = java.net.URLDecoder.decode(repoUrl, StandardCharsets.UTF_8.name());
            return repoCache.getRepoData(decodedRepoUrl)
                    .map(repoData -> new ResponseEntity<>(repoData.getMindMap(), HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.OK));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/repo/contains")
    public ResponseEntity<String> containsRepoData(@RequestParam String repoName) {
        boolean exists = repoCache.containsRepoData(repoName);
        return exists
                ? new ResponseEntity<>("Repository data exists.", HttpStatus.OK)
                : new ResponseEntity<>("Repository data does not exist.", HttpStatus.OK);
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
            return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        }
    }

    @PostMapping("/repo/getNodeInfo")
    public ResponseEntity<ClassOrInterfaceNode> getNodeInfo (@RequestBody Map<String, String> request) {
        try {
            ClassOrInterfaceNode node = repoCache.getRepoData(request.get("repoUrl"))
                    .orElseThrow(() -> new NoSuchElementException("Repository data not found"))
                    .getMindMap()
                    .get(request.get("nodeKey"));

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
                return new ResponseEntity<>(null,HttpStatus.OK);
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
                return new ResponseEntity<>(null, HttpStatus.OK);
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
    @PostMapping("/repo/fetch-methods")
    public ResponseEntity<List<MethodNode>> fetchMethodList(@RequestBody Map<String, String> request) {
        String repoUrl = request.get("repoUrl");
        String methodNodeKey = request.get("methodNodeKey");
        if (repoUrl == null || methodNodeKey == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<List<MethodNode>> methodNodes = relatedNodeCache.getFetchMethodList(repoUrl, methodNodeKey);
        if (methodNodes.isPresent()) {
            return new ResponseEntity<>(methodNodes.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ArrayList<>(),HttpStatus.OK);
        }
    }

}