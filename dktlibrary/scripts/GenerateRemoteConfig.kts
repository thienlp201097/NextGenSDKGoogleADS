import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

val xmlFile = File("../app/src/main/res/xml/remote_config_defaults.xml")
val outputFile = File("../app/src/main/java/com/ads/detech/config/RemoteConfig.kt")

val builder = StringBuilder()
builder.appendLine("package com.example.config")
builder.appendLine()
builder.appendLine("object RemoteConfig {")

val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
val entries = doc.getElementsByTagName("entry")

fun toScreamingSnakeCase(input: String): String {
    return input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace(Regex("[^A-Za-z0-9]"), "_")
        .uppercase()
}

for (i in 0 until entries.length) {
    val entry = entries.item(i)
    val key = entry.childNodes.item(1).textContent.trim()
    val value = entry.childNodes.item(3).textContent.trim()
    val constKey = toScreamingSnakeCase(key)
    builder.appendLine("    const val $constKey = \"$value\"")
}

builder.appendLine("}")
outputFile.parentFile?.mkdirs()
outputFile.writeText(builder.toString())

println("✅ RemoteConfig.kt đã được tạo tại: ${outputFile.absolutePath}")