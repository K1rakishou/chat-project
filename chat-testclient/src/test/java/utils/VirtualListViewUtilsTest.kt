package utils

import javafx.collections.FXCollections
import org.junit.Assert.*
import org.junit.Test
import tornadofx.onChange

class VirtualListViewUtilsTest {

  @Test
  fun `test add items in the beginning`() {
    val list = FXCollections.observableArrayList(1, 2, 3, 4)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          3,
          change
        )

        assertEquals(6, newSelectedItemIndex)
      }
    }

    list.addAll(0, listOf(5, 6, 7))
  }

  @Test
  fun `test add items in the middle`() {
    val list = FXCollections.observableArrayList(1, 2, 3, 4)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          3,
          change
        )

        assertEquals(6, newSelectedItemIndex)
      }
    }

    list.addAll(2, listOf(5, 6, 7))
  }

  @Test
  fun `test add items in the end`() {
    val list = FXCollections.observableArrayList(1, 2, 3, 4)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          3,
          change
        )

        assertEquals(3, newSelectedItemIndex)
      }
    }

    list.addAll(list.size, listOf(5, 6, 7))
  }

  @Test
  fun `test remove items from the beginning`() {
    val list = FXCollections.observableArrayList(4, 4, 3, 4, 5, 6, 7)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          6,
          change
        )

        assertEquals(3, newSelectedItemIndex)
      }
    }

    list.remove(0, 3)
  }

  @Test
  fun `test remove items from the middle`() {
    val list = FXCollections.observableArrayList(1, 2, 3, 4)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          3,
          change
        )

        assertEquals(6, newSelectedItemIndex)
      }
    }

    list.addAll(2, listOf(5, 6, 7))
  }

  @Test
  fun `test remove items from the end`() {
    val list = FXCollections.observableArrayList(1, 2, 3, 4)

    list.onChange { change ->
      while (change.next()) {
        val newSelectedItemIndex = VirtualListViewUtils.correctSelectedItemIndex<Int>(
          3,
          change
        )

        assertEquals(3, newSelectedItemIndex)
      }
    }

    list.addAll(list.size, listOf(5, 6, 7))
  }
}