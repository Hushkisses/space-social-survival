plugins {
    java
}

repositories {
    maven(url = "https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    implementation(project(":core"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.123-stable")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)

    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

// DEV-001 keeps one deployable plugin JAR while the core remains separately testable.
tasks.jar {
    archiveBaseName.set("space-social-survival")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(project(":core").tasks.named("classes"))

    from(project(":core").layout.buildDirectory.dir("classes/java/main"))
    from(project(":core").layout.buildDirectory.dir("resources/main"))
}
