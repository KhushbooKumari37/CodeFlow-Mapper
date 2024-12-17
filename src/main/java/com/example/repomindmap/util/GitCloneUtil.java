package com.example.repomindmap.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import java.io.File;
@Component
public class GitCloneUtil {

    private static final String PATH_NAME = "repos/";

    public static File cloneRepository(String repoUrl) {
        try {
            String[] urlSep = repoUrl.split("/");
            String repoName = urlSep[urlSep.length-1];
            File localRepo = new File(PATH_NAME + repoName);
            if (!localRepo.exists()) {
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(localRepo)
                        .call();
            }
            System.out.println("Repository cloned to: " + localRepo.getAbsolutePath());
            return localRepo;
        } catch (GitAPIException e) {
            System.err.println("Error while cloning repository: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
