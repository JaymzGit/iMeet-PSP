pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.onesignal.com/repo/") }
        maven { url = uri("https://www.jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.onesignal.com/repo/") }
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "iMeet PSP"
include(":app")
