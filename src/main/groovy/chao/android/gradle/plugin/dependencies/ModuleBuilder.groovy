package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.Constant
import chao.android.gradle.plugin.api.SettingsInject
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.initialization.DefaultProjectDescriptor
import org.gradle.initialization.ProjectDescriptorRegistry

/**
 * @author qinchao
 *
 * @since 2019/7/1 
 */
class ModuleBuilder {

    private String name

    private String remote

    private String project

    private boolean disabled = false

    /**
     *  todo
     */
    private String buildScope

    /**
     *  todo
     */
    private String flavorScope

    private boolean useProject

    private ModuleHandler handler

    ModuleBuilder(ModuleHandler handler) {
        this.handler = handler
    }

    ModuleBuilder name(String name) {
        this.name = name
        return this
    }

    ModuleBuilder remote(String remote) {
        this.remote = remote
        return this
    }

    ModuleBuilder project(String project) {
        this.project = project
        return this
    }

    /**
     * 将这个project载入到项目
     * @return
     */
    ModuleBuilder include() {
        if (!disabled) {
//            handler.project(name, project)
            handler.settings.include(handler.moduleParent + project)
            useProject = true
        }
        return this
    }

    ModuleBuilder exclude() {
        if (useProject) {
            ProjectDescriptorRegistry registry = handler.settings.getProjectDescriptorRegistry()
            DefaultProjectDescriptor projectDescriptor = registry.getProject(handler.moduleParent + project)
            if (projectDescriptor != null) {
                String projectPath = projectDescriptor.toString()
                ProjectDescriptor parentDescriptor = projectDescriptor.getParent()
                if (parentDescriptor != null) {
                    parentDescriptor.children.remove(projectDescriptor)
                }
                registry.removeProject(projectPath)
            }
        }
        useProject = false
        return this
    }

    ModuleBuilder onlyDebug() {
        this.buildScope = Constant.buildType.DEBUG
        return this
    }

    ModuleBuilder onlyRelease() {
        this.buildScope = Constant.buildType.RELEASE
    }

    ModuleBuilder builderScope(String scope) {
        this.buildScope = scope
        return this
    }

    ModuleBuilder flavorScope(String scope) {
        this.flavorScope = scope
        return this
    }

    ModuleBuilder disabled() {
        this.disabled = true
        exclude()
        return this
    }

    ModuleBuilder enabledByProperty(String property) {
        println("${property} is ${SettingsInject.props.propertyResult(property).value}")

        if (!SettingsInject.props.propertyResult(property).match('true') ) {
            println("abkit: module disabled because of not match ${property}")
            disabled()
        } else {
            println("abkit: module enabled because of match ${property}")
        }
        return this
    }

    ModuleBuilder disabledByProperty(String property) {
        if (SettingsInject.props.propertyResult(property).match('true')
                || SettingsInject.props.propertyResult(property).match('1')) {
            disabled()
        }
        return this
    }

    String getName() {
        return name
    }

    boolean isDisabled() {
        return disabled
    }

    Module build() {
        Module module = new Module()
        module.name = name
        module.remote = remote
        module.useProject = useProject
        module.project = handler.moduleParent + project
        module.flavorScope = flavorScope
        module.buildScope = buildScope
        module.disabled = disabled
        return module
    }
}
