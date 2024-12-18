package com.example.repomindmap.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassOrInterfaceNode {
  private String name;
  private String packageName;
  private String nodeKey;
  private ClassOrInterfaceNode parentNode;
  private List<ClassOrInterfaceNode> implementsNode;
  private List<ClassOrInterfaceNode> extendsNode;
  private List<MethodNode> methodList;
  private List<String> annotations;
  private List<String> modifiers;
}
