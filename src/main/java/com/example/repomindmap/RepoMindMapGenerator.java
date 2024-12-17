package com.example.repomindmap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import java.io.File;
import java.util.*;

public class RepoMindMapGenerator {

    public static Map<String, List<Object>> generateMindMap(File directory) {
        Map<String, List<Object>> mindMap = new LinkedHashMap<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Process subdirectories recursively
                    Map<String, List<Object>> subMap = generateMindMap(file);
                    subMap.forEach((key, value) -> mindMap.put(key, value));
                } else if (file.getName().endsWith(".java")) {
                    // Parse Java files
                    parseJavaFile(file, mindMap);
                }
            }
        }
        return mindMap;
    }

    private static void parseJavaFile(File file, Map<String, List<Object>> mindMap) {
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(file).getResult().orElseThrow(() -> new RuntimeException("Failed to parse Java file"));

            // Extract package information
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("No Package");

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classOrInterface -> {
                String className = classOrInterface.getNameAsString();

                // List to hold information for the mind map (class, methods, annotations)
                List<Object> classInfo = new ArrayList<>();
                classInfo.add("Class");

                // Add package information
                classInfo.add(packageName);

                // Add annotations on the class
                List<String> classAnnotations = new ArrayList<>();
                for (AnnotationExpr annotation : classOrInterface.getAnnotations()) {
                    classAnnotations.add(annotation.getNameAsString());
                }
                classInfo.add(classAnnotations);

                // Add methods in the class
                List<String> methodNames = new ArrayList<>();
                classOrInterface.getMethods().forEach(method -> {
                    String methodName = method.getNameAsString();
                    methodNames.add(methodName);

                    // Add annotations for each method
                    List<String> methodAnnotations = new ArrayList<>();
                    for (AnnotationExpr annotation : method.getAnnotations()) {
                        methodAnnotations.add(annotation.getNameAsString());
                    }

                    // Map method annotations
                    mindMap.put(methodName, new ArrayList<>(Collections.singletonList(methodAnnotations)));
                });

                // Add methods to classInfo
                classInfo.add(methodNames);

                // Add this class and its information to the mind map
                mindMap.put(className, classInfo);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
