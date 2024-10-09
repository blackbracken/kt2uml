package black.bracken.kt2uml.util

import java.util.logging.Level
import java.util.logging.Logger

object Kt2umlLogger {

  private val logger = Logger.getLogger("Kt2uml")

  private fun String.warn() {
    logger.log(Level.WARNING, this)
  }

  fun <T> T.withWarn(log: String): T {
    log.warn()
    return this
  }

}
