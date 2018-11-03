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

class VirtualListView<T>(
  private val items: ObservableList<T>,
  private val cellFactory: (T) -> Node,
  private val onClick: (T) -> Unit
) {
  private var selectedItemIndex = -1

  private val virtualFlow = VirtualFlow.createVertical(items) { item ->
    Cell.wrapNode(SelectableNode.wrapNode(cellFactory(item)))
  }

  init {
    virtualFlow.setOnMouseClicked(this::onMouseClick)

    items.onChange {
      if (it.list.isEmpty()) {
        clearSelection()
      }
    }
  }

  private fun onMouseClick(event: MouseEvent) {
    val hitResult = virtualFlow.hit(event.x, event.y)
    if (hitResult.isCellHit) {
      val selectedItem = selectItem(hitResult.cellIndex)
      if (selectedItem != null) {
        onClick(selectedItem)
      }
    }
  }

  fun getVirtualFlow() = virtualFlow

  //TODO: this won't work when a new item is added to the list with index less than selectedItemIndex.
  //Should either use some kind of key to search for the item's index or update selectedItemIndex
  //whenever someone adds (or removes) items to the list
  fun selectItem(index: Int): T? {
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
    if (selectedItemIndex == -1) {
      return
    }

    if (items.isEmpty()) {
      selectedItemIndex = -1
      return
    }

    val cell = virtualFlow.getCell(selectedItemIndex)
    if (cell == null) {
      return
    }

    cell.node.clearSelection()
    selectedItemIndex = -1
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