package chao.android.gradle.plugin.dependencies


import org.gradle.api.Action
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.initialization.ClassLoaderScope
import org.gradle.api.internal.initialization.ScriptHandlerFactory
import org.gradle.api.internal.plugins.DefaultObjectConfigurationAction
import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.configuration.ScriptPlugin
import org.gradle.configuration.ScriptPluginFactory
import org.gradle.groovy.scripts.ScriptSource
import org.gradle.groovy.scripts.TextResourceScriptSource
import org.gradle.initialization.DefaultSettings
import org.gradle.internal.resource.TextResource
import org.gradle.invocation.DefaultGradle
import org.gradle.util.GradleVersion

import java.lang.reflect.Field

class ScriptHelper {

    private static final VERSION_6 = GradleVersion.version("6.0")

    private static Field scriptHandlerFactoryField

    private static Field configureFactoryField

    static {
        try {
            scriptHandlerFactoryField = DefaultObjectConfigurationAction.class.getDeclaredField("scriptHandlerFactory")
            configureFactoryField = DefaultObjectConfigurationAction.class.getDeclaredField("configurerFactory")
            scriptHandlerFactoryField.setAccessible(true)
            configureFactoryField.setAccessible(true)
        } catch (Throwable e) {
            e.printStackTrace()
        }
    }

    static void applyModuleScript(List<File> modulesFiles, DefaultGradle gradle, DefaultSettings settings, ModuleHandler moduleHandler) {
        gradle.apply(new Action<ObjectConfigurationAction>() {
            @Override
            void execute(ObjectConfigurationAction objectConfigurationAction) {

                try {
                    ScriptHandlerFactory scriptHandlerFactory = (ScriptHandlerFactory) scriptHandlerFactoryField.get(objectConfigurationAction)
                    ScriptPluginFactory configurerFactory = configureFactoryField.get(objectConfigurationAction)

                    for (File modulesFile: modulesFiles) {
                        if (!modulesFile.exists()) {
                            println("abkit Warning: file modules.gradle not exists, file path:" + modulesFile.absolutePath)
                            continue
                        }
                        TextResource settingsResource
                        if (GradleVersion.current() >= VERSION_6) {
                            Class<?> resourceLoaderClass = Class.forName("org.gradle.internal.resource.DefaultTextFileResourceLoader")
                            settingsResource = resourceLoaderClass.newInstance().loadFile(modulesFile.getAbsolutePath(), modulesFile)
                        } else {
                            Class<?> resourceLoaderClass = Class.forName("org.gradle.internal.resource.BasicTextResourceLoader")
                            settingsResource = resourceLoaderClass.newInstance().loadFile(modulesFile.getAbsolutePath(), modulesFile)
                        }
                        ScriptSource settingsScriptSource = new TextResourceScriptSource(settingsResource)
                        ClassLoaderScope settingsClassLoaderScope = settings.getClassLoaderScope()
                        ScriptHandler scriptHandler = scriptHandlerFactory.create(settingsScriptSource, settingsClassLoaderScope)

                        ClassLoaderScope classLoaderScope = settingsClassLoaderScope.createChild("script-module-inject")
                        ScriptPlugin configurer = configurerFactory.create(settingsScriptSource, scriptHandler, classLoaderScope, settingsClassLoaderScope, true)
                        configurer.apply(moduleHandler)
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace()
                } catch (IllegalAccessException e) {
                    e.printStackTrace()
                }

            }
        })
    }

}