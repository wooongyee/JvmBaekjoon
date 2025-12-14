plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // ✅ gradle.properties에서 읽어옴
        intellijIdea(providers.gradleProperty("platformVersion"))

        // ✅ bundledPlugins (복수형!) - 쉼표로 구분된 문자열을 리스트로 변환
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // 외부 플러그인 (있으면)
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',').filter { p -> p.isNotEmpty() } })

        // Bundled 모듈 (있으면)
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',').filter { m -> m.isNotEmpty() } })
    }

    implementation(libs.okhttp)
    implementation(libs.jsoup)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = "253.*"
        }
    }

    buildSearchableOptions = false
}
