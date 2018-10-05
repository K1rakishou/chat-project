package core.collections

import org.junit.Assert.*
import org.junit.Test

class RingBufferTest {
  val ringBuffer = RingBuffer<Int>(10)

  @Test
  fun `emptyCount should be zero when added ten elements`() {
    ringBuffer.add(1)
    ringBuffer.add(2)
    ringBuffer.add(3)
    ringBuffer.add(4)
    ringBuffer.add(5)
    ringBuffer.add(6)
    ringBuffer.add(7)
    ringBuffer.add(8)
    ringBuffer.add(9)
    ringBuffer.add(10)

    assertEquals(0, ringBuffer.emptyCount)
  }

  @Test
  fun `emptyCount should still be zero when added more than ten elements and headIndex should not exceed size`() {
    ringBuffer.add(1)
    ringBuffer.add(2)
    ringBuffer.add(3)
    ringBuffer.add(4)
    ringBuffer.add(5)
    ringBuffer.add(6)
    ringBuffer.add(7)
    ringBuffer.add(8)
    ringBuffer.add(9)
    ringBuffer.add(10)
    ringBuffer.add(11)
    ringBuffer.add(12)
    ringBuffer.add(13)

    assertEquals(0, ringBuffer.emptyCount)
    assertEquals(true, ringBuffer.headIndex <= ringBuffer.size)
  }

  @Test
  fun `should not return more than (size - emptyCount) elements when buffer is not full`() {
    ringBuffer.add(1)
    ringBuffer.add(2)
    ringBuffer.add(3)
    ringBuffer.add(4)

    assertEquals(4, ringBuffer.getAll().size)
  }

  @Test
  fun `should return first element`() {
    ringBuffer.add(1)

    assertEquals(1, ringBuffer.getAll()[0])
  }

  @Test
  fun `should return first element 2`() {
    ringBuffer.add(1)
    ringBuffer.add(2)
    ringBuffer.add(3)
    ringBuffer.add(4)
    ringBuffer.add(5)
    ringBuffer.add(6)
    ringBuffer.add(7)
    ringBuffer.add(8)
    ringBuffer.add(9)
    ringBuffer.add(10)

    assertEquals(1, ringBuffer.getAll()[0])
  }

  @Test
  fun `elements should be in ascending order`() {
    ringBuffer.add(1)
    ringBuffer.add(2)
    ringBuffer.add(3)
    ringBuffer.add(4)

    val elements = ringBuffer.getAll()

    assertEquals(1, elements[0])
    assertEquals(2, elements[1])
    assertEquals(3, elements[2])
    assertEquals(4, elements[3])
  }

  @Test
  fun `should overwrite elements circularly`() {
    for (i in 0 until 100) {
      ringBuffer.add(i)
    }

    val elements = ringBuffer.getAll()

    assertEquals(90, elements[0])
    assertEquals(91, elements[1])
    assertEquals(92, elements[2])
    assertEquals(93, elements[3])
    assertEquals(94, elements[4])
    assertEquals(95, elements[5])
    assertEquals(96, elements[6])
    assertEquals(97, elements[7])
    assertEquals(98, elements[8])
    assertEquals(99, elements[9])
  }
}