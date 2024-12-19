package com.example.repomindmap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Getter
@Setter
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MethodNode {

  private String name;
  private String returnType;
  private List<String> parameterTypes;
  private List<String> exceptionTypes;
  private List<String> modifiers;
  private String signature;
  private String body;
  private List<String> annotations;
  private String className;
  private String packageName;
  private String nodeKey;
  public MethodNode(String nodeKey, String name) {
    this.nodeKey = nodeKey;
    this.name = name;
  }

}
