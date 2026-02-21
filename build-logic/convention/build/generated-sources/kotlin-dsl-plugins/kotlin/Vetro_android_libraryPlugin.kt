/**
 * Precompiled [vetro.android.library.gradle.kts][Vetro_android_library_gradle] script plugin.
 *
 * @see Vetro_android_library_gradle
 */
public
class Vetro_android_libraryPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Vetro_android_library_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
