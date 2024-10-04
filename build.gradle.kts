import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  kotlin("jvm")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

group = "black.bracken.kt2uml"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  google()
  maven("https://jitpack.io")
}

dependencies {
  implementation(compose.desktop.currentOs)
  implementation("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:0.1.0")
}

compose.desktop {
  application {
    mainClass = "black.bracken.kt2uml.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "kt2uml"
      packageVersion = "1.0.0"
    }
  }
}
