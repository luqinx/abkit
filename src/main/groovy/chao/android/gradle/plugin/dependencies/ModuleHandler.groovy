package chao.android.gradle.plugin.dependencies

import org.gradle.api.initialization.Settings
/**
 * @author qinchao
 * @since 2019/5/29
 */
class ModuleHandler {

    private Settings settings

    private ModuleBuilder moduleBuilder;

    ModuleHandler(Settings settings) {
        this.settings = settings
    }

    void include(String name, String remote, String project, boolean useProject) {
        if (useProject) {
            settings.include(project)
        }
    }
    
    void include(String project) {
    }

    void addVersion(String version) {
        versions.add(version)
    }
    
    ModuleBuilder module(String moduleName, String remote, String project) {
        if (project && !project.startsWith(":")) {
            project = ":" + project
        }
        moduleBuilder = new ModuleBuilder(this)
                .remote(remote).name(moduleName).project(project)

        return moduleBuilder
    }

    void project(String project) {
        settings.include(project)
    }

    void remote(String remote) {

    }

}
