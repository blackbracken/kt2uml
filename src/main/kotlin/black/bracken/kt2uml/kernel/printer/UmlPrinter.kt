package black.bracken.kt2uml.kernel.printer

import black.bracken.kt2uml.kernel.UmlTarget

interface UmlPrinter {

  fun printFunction(target: UmlTarget.Function): String

}
