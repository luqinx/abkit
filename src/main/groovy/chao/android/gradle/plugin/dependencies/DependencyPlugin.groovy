package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.Constant
import chao.android.gradle.plugin.api.SettingsInject
import chao.android.gradle.plugin.base.BasePlugin
import chao.android.gradle.plugin.util.StringUtils
import org.gradle.api.Project

import java.util.stream.Collectors

/**
 *
 * 检索version.properties中项目
 *
 * @author qinchao
 * @since 2018/11/14
 */
class DependencyPlugin extends BasePlugin {

    private static final String MODULES_GRADLE_FILE_NAME = "modules.gradle"

    private static final String EMPTY_REMOTE_MODULE = "chao.java.tools:empty:1.0.0"

    private static final String MODULE_VERSION_BY_MAVEN = "VERSION_BY_MAVEN"

    private ModuleHandler handler

    private List<Module> modules


    DependencyPlugin(Project project) {
        super(project)
        handler = SettingsInject.rootHandler()
    }

    @Override
    void applyRoot() {

        File moduleGradle = new File(project.getRootDir(), MODULES_GRADLE_FILE_NAME)
        if (moduleGradle.exists()) {
            modules = handler.allModules()
        } else {
            modules = new ArrayList<>()
        }

        def unVersionModules = modules.stream().filter { module ->
            StringUtils.isEmpty(module.versionName)
        }.collect(Collectors.toList())

        modules.removeAll(unVersionModules)

        getProject().rootProject.subprojects { subproject ->
            def firstSubProject = true // 根工程下的第一个子project，及com.android.application对应的project
            subproject.beforeEvaluate {
                def versionDone = unVersionModules.size() == 0
                subproject.configurations.whenObjectAdded { configuration ->
                    configuration.getDependencies().whenObjectAdded { dependency ->
                        if (versionDone) {
                            return
                        }
                        def targetModule = unVersionModules.find { m ->
                            m.groupId == dependency.group && m.artifactId == dependency.name
                        }
                        if (targetModule) {
                            targetModule.versionName = dependency.version
                            unVersionModules.remove(targetModule)
                            modules.add(targetModule)
                            logd("abkit: unversion module matches: $module")
                        }
                    }
                }

                if (versionDone) {
                    //使用Module名作为依赖入口名, xxxx
                    for (Module module : modules) {
                        def orgName = subproject.extensions.findByName(module.name)
                        if (orgName) {
                            println("??????? ====> " + orgName)
                            continue
                        }
                        if (module.disabled) {
                            subproject.extensions.add(module.name, EMPTY_REMOTE_MODULE)
                        } else if (module.useProject) {
                            subproject.extensions.add(module.name, project.project(module.project))
                        } else {
                            if (!module.remote) {
                                throw new NullPointerException("${module.name} is in remote mode, but remote aar is not configuration.")
                            }
                            subproject.extensions.add(module.name, module.remote)
                        }
                    }
                } else if (!firstSubProject) {
                    throw new IllegalStateException("There are non-version modules: $unVersionModules")
                }

                firstSubProject = false

//                afterEvaluate {
//                    project.configurations.each { configuration ->
//                        configuration.getDependencies().each { dependency ->
//                            println("after $project -----------> ${dependency.group}:${dependency.name}:${dependency.version}")
//                        }
//                    }
//                }


            }


            subproject.afterEvaluate {
//                println("-----------------------> ${subproject.path}")

//                if (subproject.hasProperty("upInfo")) {
//                    println("-----------------------> ${subproject.upInfo.version}")
//                }

                //依赖替换方案
                //详见: https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.ResolutionStrategy.html
                subproject.configurations.all { configuration ->

                    configuration.resolutionStrategy.dependencySubstitution { strategy ->
                        modules.each { module ->
                            if (module.disabled) {
                                return
                            }
//                                substitute module(value.groupId + ":" + value.artifactId) with module(value.remote)
                            String from = module.groupId + ":" + module.artifactId
                            if (module.useProject) {
                                if (subproject.path == module.project) {
                                    return
                                }
//                                    def to = rootProject.rootProject(value.projectName)
//                                    println("${subproject.path}: ${module.name} from $from ----->  ${strategy.project(module.project)}")
                                strategy.substitute(strategy.module(from)).with(strategy.project(module.project))
                            } else {
                                def to = module.remote
                                strategy.substitute(strategy.module(from)).with(strategy.module(to))
                            }
                        }
                    }
                }
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
