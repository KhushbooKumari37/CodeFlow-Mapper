package com.example.repomindmap;

import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class RepoMindMapGenerator {

    private static final String DOT = ".";

    public static Map<String, ClassOrInterfaceNode> generateMindMap(File directory) {
        Map<String, ClassOrInterfaceNode> mindMap = new LinkedHashMap<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    Map<String, ClassOrInterfaceNode> subMap = generateMindMap(file);
                    subMap.forEach((key, value) -> mindMap.put(key, value));
                } else if (file.getName().endsWith(".java")) {
                    parseJavaFile(file, mindMap);
                }
            }
        }
        return mindMap;
    }

    private static void parseJavaFile(File file, Map<String, ClassOrInterfaceNode> classInfoMap) {
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(file).getResult().orElseThrow(() -> new RuntimeException("Failed to parse Java file"));
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(unit -> {
                ClassOrInterfaceNode node = getClassOrInterfaceNode(unit);
                classInfoMap.put(node.getNodeKey(), node);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ClassOrInterfaceNode getClassOrInterfaceNode(ClassOrInterfaceDeclaration unit) {
        String packageName = unit.getMetaModel().getPackageName();
        String name = unit.getNameAsString();
        ClassOrInterfaceNode node = ClassOrInterfaceNode.builder().packageName(packageName).name(name).nodeKey(packageName+ DOT +name).build();
        List<ClassOrInterfaceNode> extendNodes = new ArrayList<>();
        unit.getExtendedTypes().stream().findFirst().ifPresent(extendedNode -> {
            ClassOrInterfaceNode extendNode = getRelatedNodes(extendedNode);
            //TODO: Recurrion approach for parent data
            extendNodes.add(extendNode);
        });
        node.setExtendsNode(extendNodes);
        List<ClassOrInterfaceNode>  implementNodes = new ArrayList<>();
        unit.getImplementedTypes().forEach(parent -> {
            ClassOrInterfaceNode implementNode = getRelatedNodes(parent);
            //TODO: Recursion approach for multi hierarchy
            implementNodes.add(implementNode);
        });
        node.setImplementsNode(implementNodes);
        // Find methods
        List<MethodNode> methods = new ArrayList<>();
        methodExtractor(unit, methods);
        node.setMethodList(methods);
        //TODO: Figure out on memebers & methods
        node.setModifiers(unit.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).collect(Collectors.toList()));
        node.setAnnotations(unit.getAnnotations().stream().map(annotationExpr -> annotationExpr.getName().asString()).collect(Collectors.toList()));
        return node;
    }

    private static ClassOrInterfaceNode getRelatedNodes(ClassOrInterfaceType extendedNode) {
        ClassOrInterfaceNode extendNode = new ClassOrInterfaceNode();
        extendNode.setName(extendedNode.getNameAsString());
        extendNode.setPackageName(extendedNode.getMetaModel().getPackageName());
        extendNode.setNodeKey(extendNode.getPackageName()+DOT+extendNode.getName());
        return extendNode;
    }

    private static void methodExtractor(ClassOrInterfaceDeclaration unit, List<MethodNode> methods) {
        unit.getMethods().forEach(method -> {
            MethodNode methodNode = MethodNode.builder()
                    .name(method.getNameAsString())  // Set the method name
                    .returnType(method.getType().asString())  // Set the return type
                    .parameterTypes(method.getParameters().stream()
                            .map(parameter -> parameter.getType().asString()) // Get the type of each parameter
                            .collect(Collectors.toList()))  // Collect them into a list
                    .exceptionTypes(method.getThrownExceptions().stream()
                            .map(exception -> {
                                try {
                                    return Class.forName(exception.getClass().getName());  // Convert exception name to Class type
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }).distinct()
                            .toArray(Class<?>[]::new))
                    .modifiers(method.getModifiers().stream()
                            .map(modifier -> modifier.getKeyword().asString())
                            .collect(Collectors.toList()))
                    .signature(method.getDeclarationAsString())  // Get the method signature (name + parameters + return type)
                    .annotations(method.getAnnotations().stream().map(annotationExpr -> annotationExpr.getName().asString()).collect(Collectors.toList()))
                    .body(method.getBody().map(BlockStmt::toString).orElse(""))
                    .build();
            System.out.println(methodNode);
            methods.add(methodNode);
        });
    }
}
