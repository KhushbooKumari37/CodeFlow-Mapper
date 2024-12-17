package com.example.repomindmap;

import com.example.repomindmap.util.GitCloneUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class RepoMindMapGeneratorApplication implements CommandLineRunner {

    @Autowired
    private GitCloneUtil gitCloneUtil;

    public static void main(String[] args) {
        SpringApplication.run(RepoMindMapGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String repoUrl = "https://github.com/alexmanrique/spring-boot-application-example.git";

        try {
            String mindMapJson = generateMindMap(repoUrl);
            System.out.println("Generated Mind Map:");
            System.out.println(mindMapJson);
        } catch (Exception e) {
            System.err.println("Error generating mind map: " + e.getMessage());
        }
    }

    public String generateMindMap(String repoUrl) {
        File clonedRepo = gitCloneUtil.cloneRepository(repoUrl);
        Map<String, List<Object>> mindMap = RepoMindMapGenerator.generateMindMap(clonedRepo);
        return new Gson().toJson(mindMap);
    }
}
