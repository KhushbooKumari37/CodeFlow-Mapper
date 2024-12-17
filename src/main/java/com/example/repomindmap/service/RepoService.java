package com.example.repomindmap.service;

import com.example.repomindmap.RepoMindMapGenerator;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.util.GitCloneUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.TRANSIENT;

@Service
public class RepoService {
  @Autowired
  private GitCloneUtil gitCloneUtil;
  public Map<String, ClassOrInterfaceNode> generateMindMap(String repoUrl) {
    File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
    Map<String, ClassOrInterfaceNode> mindMap = RepoMindMapGenerator.generateMindMap(clonedRepo);
    return mindMap;
  }
}