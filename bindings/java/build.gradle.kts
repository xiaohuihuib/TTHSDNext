plugins {
    kotlin("jvm") version "2.1.0"
    `java-library`
    `maven-publish`
}

group = "com.tthsd"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // JNA - Java Native Access，用于调用 TTHSD C ABI 动态库（桌面/服务端路径）
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")

    // JSON 解析（回调数据）
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

// 将动态库文件打包进 jar 的 /native/<platform>/ 路径
tasks.processResources {
    from(rootProject.file("../../jniLibs")) {
        into("native")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("TTHSD Java/Kotlin Binding")
                description.set("Java/Kotlin binding for TTHSD high-speed downloader")
                licenses {
                    license {
                        name.set("AGPL-3.0")
                    }
                }
            }
        }
    }
}
