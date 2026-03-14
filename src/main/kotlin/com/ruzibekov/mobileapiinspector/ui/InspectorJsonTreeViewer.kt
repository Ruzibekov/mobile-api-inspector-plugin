package com.ruzibekov.mobileapiinspector.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class InspectorJsonTreeViewer : JPanel(BorderLayout()), InspectorDetailViewer {
  private val treeRoot = DefaultMutableTreeNode("Empty")
  private val treeModel = DefaultTreeModel(treeRoot)
  private val tree = JTree(treeModel)
  private var content: String = ""

  init {
    border = JBUI.Borders.empty()
    tree.isRootVisible = true
    add(JBScrollPane(tree), BorderLayout.CENTER)
  }

  override fun setContent(
    value: String,
    useJsonHighlighting: Boolean
  ) {
    content = value
    rebuildTree(value)
  }

  override fun highlight(query: String): Int {
    tree.clearSelection()

    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) {
      return 0
    }

    val matchedPaths = mutableListOf<TreePath>()
    val rootNode = treeModel.root as? DefaultMutableTreeNode ?: return 0
    collectMatches(rootNode, normalizedQuery, matchedPaths)

    matchedPaths.forEach(tree::expandPath)
    matchedPaths.firstOrNull()?.let { firstPath ->
      tree.selectionPath = firstPath
      tree.scrollPathToVisible(firstPath)
    }

    return matchedPaths.size
  }

  override fun dispose() = Unit

  private fun rebuildTree(value: String) {
    val rootNode = buildRootNode(value)
    treeModel.setRoot(rootNode)
    treeModel.reload()
    expandTopLevel(rootNode)
  }

  private fun buildRootNode(value: String): DefaultMutableTreeNode {
    if (value.isBlank()) {
      return DefaultMutableTreeNode("Empty")
    }

    val parsedJson = runCatching {
      Json.parseToJsonElement(value)
    }.getOrNull()

    return if (parsedJson == null) {
      DefaultMutableTreeNode(value)
    } else {
      buildNode(name = "JSON", element = parsedJson)
    }
  }

  private fun buildNode(
    name: String,
    element: JsonElement
  ): DefaultMutableTreeNode {
    return when (element) {
      is JsonObject -> {
        val node = DefaultMutableTreeNode(name)
        element.forEach { (key, value) ->
          node.add(buildNode(key, value))
        }
        node
      }
      is JsonArray -> {
        val node = DefaultMutableTreeNode("$name [${element.size}]")
        element.forEachIndexed { index, value ->
          node.add(buildNode("[$index]", value))
        }
        node
      }
      is JsonPrimitive -> DefaultMutableTreeNode("$name: ${element.content}")
      JsonNull -> DefaultMutableTreeNode("$name: null")
    }
  }

  private fun collectMatches(
    node: DefaultMutableTreeNode,
    query: String,
    matchedPaths: MutableList<TreePath>
  ) {
    if (node.userObject.toString().lowercase().contains(query)) {
      matchedPaths += TreePath(node.path)
    }

    val childCount = node.childCount
    for (index in 0 until childCount) {
      collectMatches(node.getChildAt(index) as DefaultMutableTreeNode, query, matchedPaths)
    }
  }

  private fun expandTopLevel(rootNode: DefaultMutableTreeNode) {
    tree.expandPath(TreePath(rootNode.path))
    val childCount = rootNode.childCount
    for (index in 0 until childCount) {
      tree.expandPath(TreePath((rootNode.getChildAt(index) as DefaultMutableTreeNode).path))
    }
  }
}
