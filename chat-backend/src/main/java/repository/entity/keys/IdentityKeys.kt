package repository.entity.keys

import org.jetbrains.exposed.sql.Table

object IdentityKeys : Table() {
  val userUuid = varchar("uuid", 64).primaryKey()
}