package utils

object VirtualListViewUtils {

  //TODO: tests
  fun correctSelectedItemIndex(
    oldSelectedIndex: Int,
    wasAdded: Boolean,
    wasRemoved: Boolean,
    changeUpperBound: Int,
    range: IntRange,
    count: Int
  ): Int {
    if (oldSelectedIndex == -1) {
      return oldSelectedIndex
    }

    var newSelectedIndex = oldSelectedIndex

    // x - item in the list
    // v - inserted item
    // sx - selected item
    // sv - selected inserted item

    if (wasAdded) {
      //let's say we have 4 items in the list and the last one is selected:
      // x x x sx
      if (oldSelectedIndex in range) {
        //then 3 new items are getting added after the 3rd one:
        // x x x sv v v x

        //now 4th item is selected which is wrong so we need to correct that
        //by adding (changeUpperBound - selectedItemIndex) to selectedItemIndex:
        // x x x v v v sx

        newSelectedIndex = oldSelectedIndex + (changeUpperBound - oldSelectedIndex)
      } else if (changeUpperBound < oldSelectedIndex) {
        //now let's say 3 new items getting added to the head of the list:
        // v v v sx x x x

        //now 4th element is selected instead of 7th so we need to correct that
        //by adding amount of added items from selectedItemIndex:
        // v v v x x x sx

        newSelectedIndex = oldSelectedIndex + count
      }

      //if the items were inserted after selectedItemIndex - we don't need to do anything
    }

    if (wasRemoved) {
      //do the same
      if (oldSelectedIndex in range) {
        newSelectedIndex = oldSelectedIndex - (changeUpperBound - oldSelectedIndex)
      } else if (oldSelectedIndex < changeUpperBound) {
        newSelectedIndex = oldSelectedIndex - count
      }
    }

    return newSelectedIndex
  }

}