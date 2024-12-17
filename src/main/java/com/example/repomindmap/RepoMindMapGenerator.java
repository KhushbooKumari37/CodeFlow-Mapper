package com.example.repomindmap;

import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class RepoMindMapGenerator {

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
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("No Package");
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(unit -> {
                String name = unit.getNameAsString();
                ClassOrInterfaceNode node = ClassOrInterfaceNode.builder().packageName(packageName).name(name).build();
                List<ClassOrInterfaceNode> interfaces = new ArrayList<>();
                unit.getExtendedTypes().stream().findFirst().ifPresent(interfaceType -> {
                    ClassOrInterfaceNode interfaceNode = new ClassOrInterfaceNode();
                    interfaceNode.setName(interfaceType.getNameAsString());
                    interfaceNode.setPackageName(interfaceType.getMetaModel().getPackageName());
                    //TODO: Recurrion approach for parent data
                    interfaces.add(interfaceNode);
                });

                unit.getImplementedTypes().forEach(parent -> {
                    node.setContainsParent(true);
                    ClassOrInterfaceNode parentNode = new ClassOrInterfaceNode();
                    parentNode.setName(parent.getNameAsString());
                    parentNode.setContainsParent(parent.hasParentNode());
                    parentNode.setPackageName(parent.getMetaModel().getPackageName());
                    if (parentNode.isContainsParent()) {
                        //TODO: Recursion approach for multi hierarchy
                        //parentNode.setParentNode(parent.getParentNode());
                    }
                    node.setParentNode(parentNode); // Set parent node here
                });
                if(!interfaces.isEmpty()) {
                    node.setUsesInterface(true);
                    node.setInterfaceNodeList(interfaces);
                }
                // Find methods
                List<MethodNode> methods = new ArrayList<>();
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
                                    })
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
                node.setMethodList(methods);
                //TODO: Figure out on memebers & methods
                node.setModifiers(unit.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).collect(Collectors.toList()));
                node.setAnnotations(unit.getAnnotations().stream().map(annotationExpr -> annotationExpr.getName().asString()).collect(Collectors.toList()));
                classInfoMap.put(packageName + name, node);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
