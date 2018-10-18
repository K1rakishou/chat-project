package core.security

import org.junit.Assert.*
import org.junit.Test

class SecurityUtilsTest {

  @Test
  fun `test sha3-384`() {
    val expected = "f39de487a8aed2d19069ed7a7bcfc274e9f026bba97c8f059be6a2e5eed051d7ee437b93d80aa6163bf8039543b612dd".toUpperCase()
    val actual = SecurityUtils.Hashing.sha3("1")

    assertEquals(expected, actual)
  }

  @Test
  fun `test sha3-384 2`() {
    val expected = "0536ed462551cc68c6d1bb10c28e95d740fb72184056065b17a7d19d03295704f1db016611dc058ed84172b278c816c6".toUpperCase()
    val actual = SecurityUtils.Hashing.sha3("122222222222222222222444444444444444444444444555555555555555555555666666666666666666666667777777777777777777777788888888888888888888888899999999999999999995666666666666666")

    assertEquals(expected, actual)
  }
}