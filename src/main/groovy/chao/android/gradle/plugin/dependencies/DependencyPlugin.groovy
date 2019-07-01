package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.base.BasePlugin
import chao.android.gradle.plugin.Constant
import org.gradle.api.Project
/**
 *
 * 检索version.properties中项目
 *
 * @author qinchao
 * @since 2018/11/14
 */
class DependencyPlugin extends BasePlugin {


    private static final List<String> DEPENDENCY_CONFIGURATION_LIST = new ArrayList<>()

    DependencyPlugin(Project project) {
        super(project)
    }

    static {
        DEPENDENCY_CONFIGURATION_LIST.add("annotationProcessor")
        DEPENDENCY_CONFIGURATION_LIST.add("api")
        DEPENDENCY_CONFIGURATION_LIST.add("compile")
        DEPENDENCY_CONFIGURATION_LIST.add("implementation")
        DEPENDENCY_CONFIGURATION_LIST.add("runtime")
        DEPENDENCY_CONFIGURATION_LIST.add("provided")
        DEPENDENCY_CONFIGURATION_LIST.add("privateApi")
    }

    @Override
    void applyRoot() {

        ModuleManager moduleManager = new ModuleManager(project)
        moduleManager.load()

        Map<String, Module> data = moduleManager.data
        Set<String> configModuleSet = data.keySet()

        getProject().rootProject.subprojects { subproject ->


            subproject.beforeEvaluate {

//                //创建我们的configuration,取名privateApi
//                configurations {
//                    privateApi.extendsFrom api
//                    privateAnnotationProcessor.extendsFrom annotationProcessor
//                }

                //使用Module名作为依赖入口名， privateApi xxxx
                for (String configModule : configModuleSet) {
                    Module module = data.get(configModule)
                    if (module.isUseProject()) {
                        subproject.extensions.add(configModule, project.project(module.project))
                    } else {
                        subproject.extensions.add(configModule, module.remote)
                    }
                }
            }


            subproject.afterEvaluate {

                //依赖替换方案
                //详见: https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.ResolutionStrategy.html
                subproject.configurations.all { configuration ->
                    configuration.resolutionStrategy.dependencySubstitution { strategy ->
                            data.findAll { key, value ->
//                                substitute module(value.groupId + ":" + value.artifactId) with module(value.remote)
                                String from = value.groupId + ":" + value.artifactId
                                if (value.useProject) {
//                                    def to = rootProject.rootProject(value.projectName)
                                    strategy.substitute(strategy.module(from)).with(strategy.project(value.project))
                                } else {
                                    def to = value.remote
                                    strategy.substitute(strategy.module(from)).with(strategy.module(to))
                                }
                            }
                    }
                }

//                subproject.configurations.getByName("privateAnnotationProcessor") { configuration ->
//                    configuration.dependencies.findAll { dependency ->
//                        data.findAll { key, value ->
//                            if (value.projectName == dependency.name) {
//                                subproject.dependencies.add("annotationProcessor", dependency)
//                            }
//                        }
//                    }
//                }
//
//                subproject.configurations.getByName("privateApi") { configuration ->
//                    configuration.dependencies.findAll { dependency ->
//                        data.findAll { key, value ->
//                            if (value.projectName == dependency.name) {
//                                subproject.dependencies.add("api", dependency)
//                            }
//                        }
//                    }
//                }

            }
        }

    }

    @Override
    String bindExtensionName() {
        return Constant.extension.AC_DEPENDENCY
    }

    @Override
    boolean enabledAsDefault() {
        return true
    }
}
