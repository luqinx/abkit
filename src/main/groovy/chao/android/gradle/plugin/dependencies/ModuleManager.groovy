package chao.android.gradle.plugin.dependencies

import org.gradle.api.Project
/**
 * @author qinchao
 * @since 2018/11/16
 */
class ModuleManager {

    static final String MODULES_FILE_NAME = "modules.xml"

    Map<String, Module> data

    Map<String, List<Module>> groupIds

    Project project


    ModuleManager(Project project) {
        this.project = project
    }

    void load() {
        File file = new File(project.getRootDir(), MODULES_FILE_NAME)
        if (file.exists()) {
            def result = ModuleXmlParser.parse(file)
            data = result[0]
            groupIds = result[1]
        } else {
            data = new HashMap<>()
            groupIds = new HashMap<>()
            printMissConfigurations()
        }
    }

    private static void printMissConfigurations() {
        System.err.println("missing modules.xml, like this:")
        System.err.println("<modules>")
        System.err.println("    <module name=\"leakcanary_android\" select=\"remote\">")
        System.err.println("       <project>leakcanary_android</project>")
        System.err.println("       <remote>com.xxxxx:leakcanary-android:1.0.0</remote>")
        System.err.println("    </module>")
        System.err.println("    ...")
        System.err.println("</modules>")
    }
}
