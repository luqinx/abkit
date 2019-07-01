package chao.android.gradle.plugin.dependencies

/**
 * @author qinchao
 * @since 2018/11/14
 */
class Module {

    String name

    String projectName

    String artifactId

    String groupId

    String version

    // remote = artifactId + : + groupId + : + version
    String remote

    boolean useProject

    @Override
    String toString() {
        return name + " -----> " + projectName + " ----> " + remote + " ---> " + useProject
    }

    String getProject() {
        return ":" + projectName
    }
}
