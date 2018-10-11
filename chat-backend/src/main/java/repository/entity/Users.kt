package repository.entity

import org.jetbrains.exposed.sql.Table

object Users : Table() {
  val uuid = varchar("uuid", 64).primaryKey()
  val userName = varchar("user_name", 64)
  val passwordHash = varchar("password_hash", 128)
}