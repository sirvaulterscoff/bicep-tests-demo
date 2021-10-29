val kotestVersion: String by project
val mockkVersion: String by project
plugins {
    kotlin("jvm") version "1.5.30"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven(url = "https://nexus-new.tcsbank.ru/repository/mvn-maven-proxy/")
    maven(url = "https://nexus-new.tcsbank.ru/repository/accounting-group/")
    maven(url = "https://nexus-new.tcsbank.ru/repository/java-commons-group/")
    maven(url = "https://nexus-new.tcsbank.ru/repository/mvn-thirdparty/")
    maven(url = "https://nexus-new.tcsbank.ru/repository/jcenter/")
    maven(url = "https://nexus-new.tcsbank.ru/repository/mvn-springio-plugins-release/")

}

tasks.withType<Test>() {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.4")

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")

}