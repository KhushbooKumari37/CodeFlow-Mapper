package com.example.repomindmap;

import com.example.repomindmap.cache.RelatedNodeCache;
import com.example.repomindmap.model.ClassOrInterfaceNode;
import com.example.repomindmap.model.MethodNode;
import com.example.repomindmap.service.RepoService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RepoMindMapGenerator {

    private static final String DOT = ".";

    @Autowired
    public RepoService repoService;
    @Autowired
    public RelatedNodeCache relatedNodeCache;

    public Map<String, ClassOrInterfaceNode> generateMindMap(File directory,String repoUrl) {
        Map<String, ClassOrInterfaceNode> mindMap = new LinkedHashMap<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    Map<String, ClassOrInterfaceNode> subMap = generateMindMap(file,repoUrl);
                    mindMap.putAll(subMap);
                } else if (file.getName().endsWith(".java")) {
                    parseJavaFile(file, mindMap,repoUrl);
                }
            }
        }
        return mindMap;
    }

    public void getFetchMethod(File directory,String repoUrl) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getFetchMethod(file,repoUrl);
                } else if (file.getName().endsWith(".java")) {
                    fetchMethodList(file, repoUrl);
                }
            }
        }
    }

    private void parseJavaFile(File file, Map<String, ClassOrInterfaceNode> classInfoMap,String repoUrl) {
        try {
            JavaParser javaParser = new JavaParser();
            CompilationUnit cu = javaParser.parse(file).getResult().orElseThrow(() -> new RuntimeException("Failed to parse Java file"));
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(unit -> {
                ClassOrInterfaceNode node = getClassOrInterfaceNode(unit, cu.getPackageDeclaration().orElseGet(PackageDeclaration::new), repoUrl);
                classInfoMap.put(node.getNodeKey(), node);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ClassOrInterfaceNode getClassOrInterfaceNode(ClassOrInterfaceDeclaration unit, PackageDeclaration packageDeclaration,String repoUrl) {
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
        methodExtractor(unit, methods, node.getPackageName(), node.getName(),repoUrl);
        List<String> methodNames =  methods.values().stream().map(m->m.getName()).collect(Collectors.toList());
        methodNames.forEach(m-> relatedNodeCache.putClassCache(repoUrl,packageName+"#"+m, node.getName()));
        node.setMethodList(methods.values().stream().collect(Collectors.toList()));
        node.setModifiers(unit.getModifiers().stream().map(modifier -> modifier.getKeyword().asString()).collect(Collectors.toList()));
        node.setAnnotations(unit.getAnnotations().stream().map(annotationExpr -> annotationExpr.getName().asString()).collect(Collectors.toList()));
        return node;
    }

    private ClassOrInterfaceNode getRelatedNodes(ClassOrInterfaceType extendedNode) {
        ClassOrInterfaceNode extendNode = new ClassOrInterfaceNode();
        extendNode.setName(extendedNode.getNameAsString());
        extendNode.setPackageName(extendedNode.getMetaModel().getPackageName());
        extendNode.setNodeKey(extendNode.getPackageName()+DOT+extendNode.getName());
        return extendNode;
    }

    private void methodExtractor(ClassOrInterfaceDeclaration unit, Map<String, MethodNode> methods, String packageName, String className,String repoUrl) {
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
            String methodKey = methodNode.getName() + "#" + methodNode.getNodeKey() + "#" + methodNode.getParameterTypes().size();
            methods.put(methodKey, methodNode);
            relatedNodeCache.put(repoUrl,methodKey, methodNode);
        });
    }

    public void fetchMethodList(File file,String repoUrl) {
        JavaParser javaParser = new JavaParser();
        CompilationUnit cu = null;
      try {
          TypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
          JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
          javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
          cu = javaParser.parse(file).getResult().orElseThrow(() -> new RuntimeException("Failed to parse Java file"));
          String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

          cu = javaParser.parse(file).getResult().orElseThrow(() -> new RuntimeException("Failed to parse Java file"));
          cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {

              String name = methodDeclaration.getNameAsString();
              List<MethodNode> methodNodes = new ArrayList<>();
              Optional<String> mainClassName = relatedNodeCache.getMethodsForClass(repoUrl,packageName+"#"+name);
              if(mainClassName.isPresent()) {
                  methodDeclaration.findAll(MethodCallExpr.class).forEach(m -> {
                      String methodName = m.getNameAsString();

                      Optional<String> className = relatedNodeCache.getMethodsForClass(repoUrl, packageName + "#" + methodName);
                      if (className.isPresent()) {
                          System.out.print(name + " method - " + methodName);
                          System.out.println(" " + className.get() + " - ");
                          String methodNodeKey = name + "#" + packageName + "." + className.get() + "#" + m.getArguments().size();
                          Optional<MethodNode> methodNode = relatedNodeCache.getMethodData(repoUrl, methodNodeKey);
                          methodNode.ifPresent(methodNodes::add);
                      }
                  });
                  String methodNodeKey = name + "#" + packageName + "." + mainClassName.get() + "#" + methodDeclaration.getParameters().size();
                  if (!methodNodes.isEmpty()) {
                      relatedNodeCache.put2(repoUrl, methodNodeKey, methodNodes);
                  }
              }
          });
      } catch (Exception e) {
         e.printStackTrace();
      }
    }
}
