package com.example.repomindmap.service;

import com.example.repomindmap.RepoMindMapGenerator;
import com.example.repomindmap.util.GitCloneUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class RepoService {
  @Autowired
  private GitCloneUtil gitCloneUtil;
  public String generateMindMap(String repoUrl) {
    File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
    Map<String, List<Object>> mindMap = RepoMindMapGenerator.generateMindMap(clonedRepo);
    return new Gson().toJson(mindMap);
  }
}

