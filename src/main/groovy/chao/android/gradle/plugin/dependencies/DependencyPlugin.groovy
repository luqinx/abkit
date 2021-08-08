package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.Constant
import chao.android.gradle.plugin.api.SettingsInject
import chao.android.gradle.plugin.base.BasePlugin
import chao.android.gradle.plugin.util.StringUtils
import org.gradle.api.Project

import java.util.concurrent.CountDownLatch
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
            StringUtils.isEmpty(module.versionName) && !module.useProject
        }.collect(Collectors.toList())

        modules.removeAll(unVersionModules)

        def versionDone = unVersionModules.size() == 0
//        println("  unversion ------> $versionDone   ${unVersionModules.size()}    ${unVersionModules}")

        getProject().rootProject.subprojects { subproject ->
            subproject.beforeEvaluate {
                println("before evaluate  ${subproject}")
                if (versionDone) {
                    configureDependency(subproject)
                } else {
                    subproject.configurations.whenObjectAdded { configuration ->
                        if (versionDone) { return }
                        configuration.getDependencies().whenObjectAdded { dependency ->
                            if (versionDone) { return }

                            // 第一个子project才应该走到这里, 第一个project是com.android.application对应的project
                            def targetModule = unVersionModules.find { m ->
                                m.groupId == dependency.group && m.artifactId == dependency.name
                            }
                            if (targetModule) {
                                targetModule.versionName = dependency.version
                                unVersionModules.remove(targetModule)
                                modules.add(targetModule)
                                println("abkit: unversion module matches: ${targetModule.remote}")
                            }
                            versionDone = unVersionModules.size() == 0
                            if (versionDone) {
                                configureDependency(subproject)
                            }
                        }
                    }
                }
            }

            subproject.afterEvaluate {
                println("after evaluate  ${subproject}")
                if (!versionDone) {
                    throw new IllegalStateException("There are non-version modules: $unVersionModules")
                }
                modules.forEach {
                    if (!it.useProject && StringUtils.isEmpty(it.remote)) {
                        throw new IllegalStateException("abkit: ${it.name} use remote dependency but remote config is empty!!")
                    } else if (it.useProject && StringUtils.isEmpty(it.project)) {
                        throw new IllegalStateException("abkit: ${it.name} use project dependency but project config is empty!!")
                    }
                }

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

    private void configureDependency(Project subproject) {
//        println("abkit: configure dependency for $subproject")
        //使用Module名作为依赖入口名, xxxx
        for (Module module : modules) {
            def orgName = subproject.extensions.findByName(module.name)
            if (orgName) {
                println("??????? ====> " + orgName)
                continue
            }
            Object result
            if (module.disabled) {
                result = EMPTY_REMOTE_MODULE
            } else if (module.useProject) {
                result = project.project(module.project)
            } else {
                if (!module.remote) {
                    throw new NullPointerException("${module.name} is in remote mode, but remote aar is not configuration.")
                }
                result = module.remote
            }
            subproject.extensions.add(module.name, result)
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
