// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.folding;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
@State(name = "CodeFoldingSettings", storages = {
  @Storage("editor.xml"),
  @Storage(value = "editor.codeinsight.xml", deprecated = true),
})
public class CodeFoldingSettingsImpl extends CodeFoldingSettings implements PersistentStateComponent<CodeFoldingSettings> {
  @Override
  public CodeFoldingSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull final CodeFoldingSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
