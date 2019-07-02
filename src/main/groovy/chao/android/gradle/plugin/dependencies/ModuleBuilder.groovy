package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.Constant

/**
 * @author qinchao
 *
 * @since 2019/7/1 
 */
class ModuleBuilder {

    private String name

    private String remote

    private String project

    private boolean useProject

    private String buildScope

    private String flavorScope

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

    ModuleBuilder useProject(boolean project) {
        this.useProject = project
        if (useProject) {
            handler.project(project)
        }
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

}
