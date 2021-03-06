/*
 * Copyright 2020 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.cliuff.boundo.dependency.Dependencies

plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    kotlin("android.extensions")
    id("kotlin-kapt")
    id("com.cliuff.boundo.dependencies")
}

android {
    sourceSets {
        getByName("main").java.srcDir("src/main/kotlin")
    }
    compileSdkVersion(30)

    flavorDimensions("arch")
    productFlavors {
        create("full") {
            setDimension("arch")
        }
        create("arm") {
            setDimension("arch")
            ndk{
                abiFilters("armeabi-v7a", "arm64-v8a")
            }
        }
        create("x86") {
            setDimension("arch")
            ndk{
                abiFilters("x86", "x86_64")
            }
        }
    }

    defaultConfig {
        minSdkVersion(22)
        targetSdkVersion(30)
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    buildFeatures.viewBinding = true
}

dependencies {
    listOf(
            fileTree(Dependencies.fileTreeValue),
            project(":app"),
            Dependencies.androidxRecyclerView,
            Dependencies.glide
    ).forEach { implementation(it) }
    Dependencies.dynamicFeatureBasics.forEach { implementation(it) }
}
