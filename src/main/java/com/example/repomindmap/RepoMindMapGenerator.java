package com.example.repomindmap;

import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.example.repomindmap.service.RepoService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class RepoMindMapGenerator {

    private static final String DOT = ".";

    @Autowired
    public static RepoService repoService;

    public static Map<String, ClassOrInterfaceNode> generateMindMap(File directory) {
        Map<String, ClassOrInterfaceNode> mindMap = new LinkedHashMap<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    Map<String, ClassOrInterfaceNode> subMap = generateMindMap(file);
                    mindMap.putAll(subMap);
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
                ClassOrInterfaceNode node = getClassOrInterfaceNode(unit, cu.getPackageDeclaration().orElseGet(PackageDeclaration::new));
                classInfoMap.put(node.getNodeKey(), node);
            });
            repoService.fetchMethodList(cu);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ClassOrInterfaceNode getClassOrInterfaceNode(ClassOrInterfaceDeclaration unit, PackageDeclaration packageDeclaration) {
        String packageName = packageDeclaration.getNameAsString();
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
        Map<String, MethodNode> methods = new HashMap<>();
        methodExtractor(unit, methods, node.getPackageName(), node.getName());
        node.setMethodList(methods.values().stream().collect(Collectors.toList()));
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

    private static void methodExtractor(ClassOrInterfaceDeclaration unit, Map<String, MethodNode> methods, String packageName, String className) {
        unit.getMethods().forEach(method -> {
            MethodNode methodNode = MethodNode.builder()
                    .name(method.getNameAsString())
                    .returnType(method.getType().asString())
                    .parameterTypes(method.getParameters().stream()
                            .map(parameter -> parameter.getType().asString())
                            .collect(Collectors.toList()))
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
                    .nodeKey(packageName+DOT+className)
                    .className(className)
                    .packageName(packageName)
                    .build();
            System.out.println(methodNode);
            String methodKey = methodNode.getName() + "#" + methodNode.getNodeKey() + "#" + methodNode.getParameterTypes().size();
            methods.put(methodKey, methodNode);
        });
    }
}
