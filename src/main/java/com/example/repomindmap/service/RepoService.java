package com.example.repomindmap.service;

import com.example.repomindmap.RepoMindMapGenerator;
import com.example.repomindmap.cache.RelatedNodeCache;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.example.repomindmap.util.GitCloneUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.reflect.Modifier.TRANSIENT;

@Service
public class RepoService {
  @Autowired
  private GitCloneUtil gitCloneUtil;
  @Autowired
  private RelatedNodeCache relatedNodeCache;

  public Map<String, ClassOrInterfaceNode> generateMindMap(String repoUrl) {
    File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
    Map<String, ClassOrInterfaceNode> mindMap = RepoMindMapGenerator.generateMindMap(clonedRepo);
    return mindMap;
  }

  @Async
  public CompletableFuture<List<MethodNode>> fetchMethodList(String repoUrl, Map<String, ClassOrInterfaceNode> mindMap) {
    List<MethodNode> methodNodeList = new ArrayList<>();

    mindMap.values().forEach(node -> {
      String nodeKey = node.getNodeKey();

      node.getMethodList().forEach(methodNode -> {
        String[] methodNodes = methodNode.getBody().split("\n");

        for (String methodBodyLine : methodNodes) {
          // Split method body by "(" to extract method names
          String[] methodSeperator = methodBodyLine.split("\\(");

          for (String methodSep : methodSeperator) {
            // Assuming the method name is at the start of the line before '('
            String methodName = methodSep.trim();

            // Check if the method exists in the cache
            Optional<MethodNode> cachedMethodNode = relatedNodeCache.getMethodData(methodName, methodKey -> {
              return null;
            });

            cachedMethodNode.ifPresent(method -> {
              method.setNodeKey(nodeKey);
              methodNodeList.add(method);
            });
          }
        }
      });
    });

    return CompletableFuture.completedFuture(methodNodeList);
  }
}
