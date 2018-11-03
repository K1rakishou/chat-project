package ui.widgets

import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.paint.Paint
import org.fxmisc.flowless.Cell
import org.fxmisc.flowless.VirtualFlow
import tornadofx.addChildIfPossible
import tornadofx.onChange
import utils.VirtualListViewUtils

class VirtualListView<T>(
  private val items: ObservableList<T>,
  private val cellFactory: (T) -> Node,
  private val keySelector: (T) -> String,
  private val onClick: (T) -> Unit
) {
  private val noSelection = -1
  private var selectedItemIndex = noSelection

  private val virtualFlow = VirtualFlow.createVertical(items) { item ->
    Cell.wrapNode(SelectableNode.wrapNode(cellFactory(item)))
  }

  init {
    virtualFlow.setOnMouseClicked(this::onMouseClick)

    items.onChange { change ->
      while (change.next()) {
        if (selectedItemIndex == noSelection) {
          break
        }

        if (change.list.isEmpty()) {
          clearSelection()
        }

        if (change.wasRemoved()) {
          if (selectedItemIndex in change.from..change.to) {
            clearSelection()
          }
        }

        selectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex(
          selectedItemIndex,
          change
        )
      }
    }
  }

  private fun onMouseClick(event: MouseEvent) {
    val hitResult = virtualFlow.hit(event.x, event.y)
    if (hitResult.isCellHit) {
      val selectedItem = selectItemInternal(hitResult.cellIndex)
      if (selectedItem != null) {
        onClick(selectedItem)
      }
    }
  }

  fun getVirtualFlow() = virtualFlow

  fun selectItemByKey(key: String) {
    val index = items.indexOfFirst { keySelector(it) == key }
    if (index == -1) {
      println("No item found with key ($key)")
      return
    }

    selectItemInternal(index)
  }

  private fun selectItemInternal(index: Int): T? {
    if (index < 0 || index > items.size) {
      throw ArrayIndexOutOfBoundsException("index ($index) if out of bounds 0..${items.size}")
    }

    if (index == selectedItemIndex) {
      return getSelectedItem()
    }

    val cell = virtualFlow.getCell(index)
    if (cell == null) {
      return null
    }

    //if previously selected item index is not the same as current - clear it's selection
    if (selectedItemIndex != index) {
      clearSelection()
    }

    //then select a new item
    cell.node.select()
    selectedItemIndex = index

    return getSelectedItem()
  }

  fun getSelectedItem(): T? {
    return items.getOrNull(selectedItemIndex)
  }

  fun clearSelection() {
    if (selectedItemIndex == noSelection) {
      return
    }

    if (items.isEmpty()) {
      selectedItemIndex = noSelection
      return
    }

    val cell = virtualFlow.getCell(selectedItemIndex)
    if (cell == null) {
      return
    }

    cell.node.clearSelection()
    selectedItemIndex = noSelection
  }

  class SelectableNode private constructor(
    private val node: Node
  ) : Region() {
    private val selectedColor = Background(BackgroundFill(Paint.valueOf("#cde4ff"), CornerRadii.EMPTY, Insets.EMPTY))
    private var isSelected = false

    init {
      addChildIfPossible(node)
    }

    fun select() {
      background = selectedColor
      isSelected = true
    }

    fun clearSelection() {
      background = null
      isSelected = false
    }

    companion object {
      fun wrapNode(node: Node): SelectableNode {
        return SelectableNode(node)
      }
    }
  }
}