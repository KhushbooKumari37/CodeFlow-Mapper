package com.example.repomindmap.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
@Component
public class GitCloneUtil {

    public static File cloneRepository(String repoUrl) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            File localRepo = new File("RepoUrl"  + timestamp);  // Corrected the file path
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localRepo)
                    .call();
            System.out.println("Repository cloned to: " + localRepo.getAbsolutePath());
            return localRepo;
        } catch (GitAPIException e) {
            System.err.println("Error while cloning repository: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
