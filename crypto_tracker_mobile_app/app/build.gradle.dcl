androidApplication {
    namespace = "org.example.app"
    
    dependencies {
        // AndroidX Core and UI Components
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.11.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.cardview:cardview:1.0.0")
        
        // Navigation Components
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
        
        // Lifecycle Components
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
        
        // Retrofit for API calls
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
        
        // Coroutines for async operations
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        
        // MPAndroidChart for price charts
        implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
        
        // Glide for image loading
        implementation("com.github.bumptech.glide:glide:4.16.0")
        
        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
        implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
        implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
        implementation("com.google.firebase:firebase-analytics-ktx")
        implementation("com.google.firebase:firebase-crashlytics-ktx")
        implementation("com.google.firebase:firebase-perf-ktx")
        
        // Room for data persistence
        implementation("androidx.room:room-runtime:2.6.1")
        implementation("androidx.room:room-ktx:2.6.1")
        kapt("androidx.room:room-compiler:2.6.1")
        
        // DataStore for preferences
        implementation("androidx.datastore:datastore-preferences:1.0.0")
        
        // Work Manager for background tasks
        implementation("androidx.work:work-runtime-ktx:2.9.0")

        // Feature flags and A/B testing
        implementation("com.google.firebase:firebase-config-ktx")
        
        // In-app feedback
        implementation("com.google.android.play:core:1.10.3")
        implementation("com.google.android.play:core-ktx:1.8.1")
        
        // Project dependencies
        implementation(project(":utilities"))
    }

    android {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
