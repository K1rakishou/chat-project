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
import org.fxmisc.flowless.VirtualFlowHit
import tornadofx.addChildIfPossible
import java.util.*

class VirtualMultiSelectListView<T>(
  private val items: ObservableList<T>,
  private val cellFactory: (T) -> Node
) {
  private var selectedItemIndexes = TreeSet<Int>()

  private val virtualFlow = VirtualFlow.createVertical(items) { item ->
    Cell.wrapNode(SelectableNode.wrapNode(cellFactory(item)))
  }

  fun onMouseClick(event: MouseEvent) {
    val hitResult = virtualFlow.hit(event.x, event.y)
    if (hitResult.isCellHit) {
      selectedItemIndexes = processSelection(event.isShiftDown, event.isControlDown, selectedItemIndexes, hitResult)
    } else {
      clearSelection()
    }
  }

  private fun processSelection(
    shiftDown: Boolean,
    ctrlDown: Boolean,
    selectedIndexes: TreeSet<Int>,
    hitResult: VirtualFlowHit<Cell<T, SelectableNode>>
  ): TreeSet<Int> {
    val cellIndex = hitResult.cellIndex

    when {
      shiftDown -> {
        if (selectedItemIndexes.isEmpty()) {
          selectItemInternal(cellIndex)
          selectedIndexes.add(cellIndex)
        }

        val lastItem = selectedItemIndexes.last()
        val from = Math.min(lastItem, cellIndex)
        val to = from + Math.abs(lastItem - cellIndex)
        val indexes = (from..to).toList()

        indexes.forEach { selectItemInternal(it) }
        selectedIndexes.addAll(indexes)
      }
      ctrlDown -> {
        selectItemInternal(cellIndex)
        selectedIndexes.add(cellIndex)
      }
      else -> {
        clearSelection()
        selectedIndexes.clear()

        selectItemInternal(cellIndex)
        selectedIndexes.add(cellIndex)
      }
    }

    return selectedIndexes
  }

  fun getVirtualFlow() = virtualFlow

  private fun selectItemInternal(index: Int) {
    if (index < 0 || index > items.size) {
      throw ArrayIndexOutOfBoundsException("index ($index) if out of bounds 0..${items.size}")
    }

    val cell = virtualFlow.getCell(index)
    if (cell == null) {
      return
    }

    cell.node.select()
  }

  fun getSelectedItems(): List<T> {
    return selectedItemIndexes.map { items[it] }
  }

  fun clearSelection() {
    if (selectedItemIndexes.isEmpty()) {
      return
    }

    if (items.isEmpty()) {
      selectedItemIndexes.clear()
      return
    }

    selectedItemIndexes
      .mapNotNull { virtualFlow.getCell(it) }
      .forEach { cell -> cell.node.clearSelection() }

    selectedItemIndexes.clear()
  }

  fun getLastItemIndex(): Int {
    return items.size
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