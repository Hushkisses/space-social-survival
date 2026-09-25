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
    implementation(project(":integrations:itemsadder"))
    implementation(project(":integrations:voicechat"))
    implementation(project(":integrations:mythicmobs"))
    implementation(project(":integrations:modelengine"))
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

tasks.jar {
    archiveBaseName.set("space-social-survival")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(project(":core").tasks.named("classes"))
    dependsOn(project(":integrations:itemsadder").tasks.named("classes"))
    dependsOn(project(":integrations:voicechat").tasks.named("classes"))
    dependsOn(project(":integrations:mythicmobs").tasks.named("classes"))
    dependsOn(project(":integrations:modelengine").tasks.named("classes"))

    from(project(":core").layout.buildDirectory.dir("classes/java/main"))
    from(project(":core").layout.buildDirectory.dir("resources/main"))
    from(project(":integrations:itemsadder").layout.buildDirectory.dir("classes/java/main"))
    from(project(":integrations:itemsadder").layout.buildDirectory.dir("resources/main"))
    from(project(":integrations:voicechat").layout.buildDirectory.dir("classes/java/main"))
    from(project(":integrations:voicechat").layout.buildDirectory.dir("resources/main"))
    from(project(":integrations:mythicmobs").layout.buildDirectory.dir("classes/java/main"))
    from(project(":integrations:mythicmobs").layout.buildDirectory.dir("resources/main"))
    from(project(":integrations:modelengine").layout.buildDirectory.dir("classes/java/main"))
    from(project(":integrations:modelengine").layout.buildDirectory.dir("resources/main"))
}
