package com.example.repomindmap.controller;
import com.example.repomindmap.service.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RepoController {

    @Autowired
    private RepoService repoService;

    @PostMapping("/generate-mindmap")
    public ResponseEntity<String> generateMindMap(@RequestBody Map<String, String> request) {
        String repoUrl = request.get("repoUrl");

        if (repoUrl == null || repoUrl.isEmpty()) {
            return new ResponseEntity<>("Repository URL is required.", HttpStatus.BAD_REQUEST);
        }

        try {
            String mindMapJson = repoService.generateMindMap(repoUrl);
            return new ResponseEntity<>(mindMapJson, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error generating mind map: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}