package com.example.repomindmap.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
  private boolean isContainsParent;
  private ClassOrInterfaceNode parentNode;
  private boolean isUsesInterface;
  private List<ClassOrInterfaceNode> interfaceNodeList;
  private List<MethodNode> methodList;
  private List<String> annotations;
  private List<String> members;
  private List<String> modifiers;
}
