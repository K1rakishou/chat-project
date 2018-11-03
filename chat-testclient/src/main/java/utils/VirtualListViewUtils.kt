package utils

import javafx.collections.ListChangeListener
import java.lang.IllegalStateException

object VirtualListViewUtils {

  fun <T> correctSelectedItemIndex(
    oldSelectedIndex: Int,
    change: ListChangeListener.Change<out T>
  ): Int {
    var newSelectedIndex = oldSelectedIndex

    // x - item in the list
    // v - inserted item
    // sx - selected item
    // sv - selected inserted item

    if (change.wasReplaced()) {
      return newSelectedIndex
    }

    if (change.wasPermutated()) {
      throw IllegalStateException("Permutation should not happen")
    }

    if (change.wasUpdated()) {
      throw IllegalStateException("Update event should not happen")
    }

    if (change.wasAdded()) {
      val range = change.from..change.to
      val count = range.last - range.first

      //let's say we have 4 items in the list and the last one is selected:
      // x x x sx
      if (oldSelectedIndex in range) {
        //then 3 new items are getting added after the 3rd one:
        // x x x sv v v x

        //now 4th item is selected which is wrong so we need to correct that:
        // x x x v v v sx

        newSelectedIndex = oldSelectedIndex + count
      } else if (change.to < oldSelectedIndex) {
        //now let's say 3 new items getting added to the head of the list:
        // v v v sx x x x

        //now 4th element is selected instead of 7th so we need to correct that:
        // v v v x x x sx

        newSelectedIndex = oldSelectedIndex + count
      }

      //if the items were inserted after selectedItemIndex - we don't need to do anything
    }

    if (change.wasRemoved()) {
      //do the same
      val range = change.from..(change.from + change.list.lastIndex)
      val count = range.last - range.first

      if (oldSelectedIndex in range) {
        newSelectedIndex = oldSelectedIndex - count
      } else if (range.endInclusive < oldSelectedIndex) {
        newSelectedIndex = oldSelectedIndex - count
      }
    }

    return newSelectedIndex
  }

}