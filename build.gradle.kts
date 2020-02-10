plugins {
    // kotlin("js") version "1.3.61"
    kotlin("multiplatform") version "1.3.61"
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

kotlin {
    /* Targets configuration omitted.
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    jvm {

    }

    js {
        this.useCommonJs()
        this.browser
    }
    /*
    macosX64() {
        binaries {
            executable {
                // Change to specify fully qualified name of your application's entry point:
                entryPoint = "sample.main"
                // Specify command-line arguments, if necessary:
                runTask?.args("")
            }
        }

        compilations.getByName("main") {
            cinterops {
                create("raylib") {
                    // Directories for header search (an analogue of the -I<path> compiler option).
                    includeDirs.allHeaders("include")
                }
            }
        }
    }
    */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        // Default source set for JVM-specific sources and dependencies:
        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:1.9.9")
                implementation("com.badlogicgames.gdx:gdx-platform:1.9.9:natives-desktop")
            }
        }

        js().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
                // implementation(npm("babylonjs", "4.0.3"))
            }
        }
    }
}
