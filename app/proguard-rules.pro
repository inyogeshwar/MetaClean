# Proguard rules for MetaClean

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Hilt / Dependency Injection
-keep class * extends dagger.hilt.internal.GeneratedComponentManager
-keep class * implements dagger.hilt.internal.GeneratedComponentManager
-dontwarn dagger.hilt.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Models
-keep class com.inyogeshwar.metaclean.data.model.** { *; }
-keep class com.inyogeshwar.metaclean.domain.model.** { *; }
-keep class com.inyogeshwar.metaclean.domain.repository.** { *; }
