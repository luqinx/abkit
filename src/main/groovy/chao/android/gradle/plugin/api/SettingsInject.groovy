package chao.android.gradle.plugin.api

import chao.android.gradle.plugin.base.Env
import chao.android.gradle.plugin.base.Property
import chao.android.gradle.plugin.dependencies.ModuleHandler
import chao.android.gradle.plugin.dependencies.ScriptHelper
import org.gradle.TaskExecutionRequest
import org.gradle.api.initialization.Settings
import org.gradle.invocation.DefaultGradle
import org.gradle.util.GradleVersion

/**
 * @author qinchao
 * @since 2019/5/29
 */
class SettingsInject {

    public static final String DEFAULT_MODULES_SETTINGS_FILE = "modules.gradle"

    private static final String MODULES_SETTINGS_FILE_KEY = "modules.gradle.file"

    public static Property props

    private static ModuleHandler rootHandler

    static void inject(Settings settings, DefaultGradle gradle) {
        new SettingsInjectHandler().inject(settings, gradle)
    }

    static class SettingsInjectHandler {

        private File[] modulesFiles

        void inject(Settings settings, DefaultGradle gradle) {
            println("abkit: current gradle version: " + GradleVersion.current())

            props = new Property()
            props.initStaticProperties(settings.getRootDir())

            String flavorValue = props.propertyResult("abkit.flavors").getValue()
            String buildTypeValue = props.propertyResult("abkit.buildTypes").getValue()

            def flavors = new HashSet<>()
            def buildTypes = new HashSet()

            flavors.addAll(flavorValue? flavorValue.split(":"): new String[0])
            buildTypes.addAll(buildTypeValue? buildTypeValue.split(":"): new String[0])

            buildTypes.add("debug")
            buildTypes.add("release")

            List<TaskExecutionRequest> requests = new ArrayList<>(gradle.startParameter.taskRequests)
            if (requests.size() == 0) {
                requests.add(null)
            }
            for (TaskExecutionRequest request:requests) {
                def args
                String syncFlavor = "" + props.propertyResult('abkit.sync.flavor').value
                String syncBuildType = "" + props.propertyResult('abkit.sync.buildType').value

                if (request == null || request.args.size() == 0) {
                    args = []
                    args.add( syncFlavor + syncBuildType)
                    println("abkit: startParameter request is empty, add config request args:" + args)
                } else {
                    args = request.args
                }
                println("abkit: startParameter request args: ${args}")
                for (String arg: args) {
                    //查找flavors
                    for (String flavor: flavors) {
                        flavor = flavor.toLowerCase()
                        if (arg != null && arg.toLowerCase().contains(flavor)) {
                            props.loadFlavorProperties(settings.getRootDir(), flavor)
                            break
                        }
                    }
                    //查找buildType
                    for (String buildType: buildTypes) {
                        buildType = buildType.toLowerCase()
                        if (arg != null && arg.toLowerCase().contains(buildType)) {
                            props.loadBuildTypeProperties(settings.getRootDir(), buildType)
                            break
                        }
                    }
                }
            }

            Env.setProperties(props)


            List<String> modulesFileNames = new ArrayList<>()
            modulesFileNames.add(DEFAULT_MODULES_SETTINGS_FILE)

            if (props.hasProperty(MODULES_SETTINGS_FILE_KEY)) {
                String modulesFileName = props.propertyResult(MODULES_SETTINGS_FILE_KEY).value

                String[] names = modulesFileName.split(";")
                for (int i = 0; i < names.size(); i++) {
                    if (!names[i].endsWith(".gradle")) {
                        names[i] += ".gradle"
                    }
                    if (!modulesFileNames.contains(names[i])) {
                        modulesFileNames.add(names[i])
                    }
                }
            }
            println("abkit: modules config files:  " + modulesFileNames)

            modulesFiles = new File[modulesFileNames.size()]
            for (int i = 0; i < modulesFileNames.size(); i++) {
                modulesFiles[i] = new File(settings.rootDir, modulesFileNames.get(i))
            }
            rootHandler = new ModuleHandler()
            rootHandler.setSettings(settings)
            rootHandler.setGradle(gradle)
            rootHandler.setModuleParent("")
            ScriptHelper.applyModuleScript(Arrays.asList(modulesFiles), gradle, settings, rootHandler)
        }
    }

    static ModuleHandler rootHandler() {
        return rootHandler
    }

}
