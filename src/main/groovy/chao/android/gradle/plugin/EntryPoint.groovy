package chao.android.gradle.plugin

import chao.android.gradle.plugin.api.SettingsInject
import chao.android.gradle.plugin.assemble.AssemblePlugin
import chao.android.gradle.plugin.base.Env
import chao.android.gradle.plugin.base.PluginManager
import chao.android.gradle.plugin.base.Property
import chao.android.gradle.plugin.dependencies.DependencyPlugin
import chao.android.gradle.plugin.dependencies.ModuleHandler
import chao.android.gradle.plugin.maven.MavenPlugin
import chao.android.gradle.plugin.normalization.CheckStylePlugin
import chao.android.gradle.plugin.normalization.JacocoPlugin
import chao.android.gradle.plugin.normalization.LintPlugin
import chao.android.gradle.plugin.version.ACVersionPlugin
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project

class EntryPoint implements Plugin<Project> {

    private PluginManager pluginManager

    @Override
    void apply(Project project) {

        if (project != project.rootProject) {
            return
        }

        if (SettingsInject.props  == null) {
            SettingsInject.props = new Property()
            SettingsInject.props.initStaticProperties(project.rootDir)
        }
        Env.setProperties(SettingsInject.props)

        Env.rootProject project

        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void buildFinished(BuildResult buildResult) {

                Env.setProperties(null)
                SettingsInject.props.clear()
                ModuleHandler.instance().clearCache()
            }
        })

        Env.debug(Env.properties.propertyResult(Constant.propertyKey.DEBUGGABLE).booleanValue(false))

        pluginManager = new PluginManager(project)

        pluginManager.registerPlugin(DependencyPlugin)
        pluginManager.registerPlugin(ACVersionPlugin)
        pluginManager.registerPlugin(MavenPlugin)
        pluginManager.registerPlugin(AssemblePlugin)
        pluginManager.registerPlugin(CheckStylePlugin)
        pluginManager.registerPlugin(LintPlugin)
        pluginManager.registerPlugin(JacocoPlugin)

        pluginManager.apply()
    }
}