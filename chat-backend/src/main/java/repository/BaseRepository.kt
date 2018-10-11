package repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import kotlin.coroutines.experimental.CoroutineContext

open class BaseRepository {
  private val dispatcher: CoroutineContext

  init {
    dispatcher = newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors() * 2, "database-pool")
  }

  fun init() {
    Database.connect(hikari())
  }

  private fun hikari(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "org.postgresql.Driver"
    config.jdbcUrl = "jdbc:postgresql://192.168.99.100:5432/postgres"
    config.username = "postgres"
    config.password = "4e7d2dfx"
    config.maximumPoolSize = 5
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
  }

  protected suspend fun <T> query(block: () -> T): T {
    return withContext(dispatcher) {
      transaction { block() }
    }
  }
}