package enchant.flare


actual class Platform actual constructor() {
    actual val platform: String = "JVM ${System.getProperty("java.version")}"
}