package repository.entity.keys

import org.jetbrains.exposed.sql.Table

object PreKeys : Table() {
  val userUuid = varchar("uuid", 64).primaryKey()
  val preKeyRecoordStructure = varchar("pre_key_record_structure", 512)
}