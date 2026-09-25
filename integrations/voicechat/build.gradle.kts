plugins {
    `java-library`
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
    compileOnly("io.papermc.paper:paper-api:26.2.build.123-stable")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
