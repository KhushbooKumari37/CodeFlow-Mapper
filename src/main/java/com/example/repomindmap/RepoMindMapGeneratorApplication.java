package com.example.repomindmap;

import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.util.GitCloneUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.TRANSIENT;

@SpringBootApplication
public class RepoMindMapGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepoMindMapGeneratorApplication.class, args);
    }
}
