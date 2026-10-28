# Gson usa reflexión sobre los DTO: se conservan sus campos para que el JSON se mapee en release.
-keep class cl.therionlabs.reservas.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Retrofit: conservar las firmas genéricas de la interfaz del API (suspend + List<T>).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface cl.therionlabs.reservas.data.remote.ReservasApi { *; }
