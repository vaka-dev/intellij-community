// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.icons.AllIcons
import com.intellij.internal.statistic.eventLog.whitelist.WhitelistStorageProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction

class UpdateWhitelistAction(val recorder: String) : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Whitelist", false) {
      override fun run(indicator: ProgressIndicator) {
        WhitelistStorageProvider.getInstance(recorder).update()
      }
    })
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    val presentation = event.presentation
    presentation.icon = AllIcons.Actions.Refresh
    presentation.text = "Update Whitelist"
  }

}