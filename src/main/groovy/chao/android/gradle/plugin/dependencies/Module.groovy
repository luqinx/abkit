package chao.android.gradle.plugin.dependencies

import chao.android.gradle.plugin.base.PluginException
import chao.android.gradle.plugin.util.StringUtils

/**
 * @author qinchao
 * @since 2018/11/14
 */
class Module {

    private String name

    private String remote

    private String project

    private String buildScope

    private String flavorScope

    private boolean useProject

    private String artifactId

    private String groupId

    private String versionName

    private boolean disabled

    String getName() {
        return name
    }

    void setName(String name) {
        this.name = name
    }

    String getRemote() {
        return remote
    }

    void setRemote(String remote) {
        this.remote = remote
        if (!StringUtils.isEmpty(remote)) {
            String[] parts = remote.split(":")
            if (parts.length < 2) {
                throw new PluginException("remote dependency format should be [groupId]:[artifactId]:[versionName], like \"chao.android.gradle:abkit:1.0.0\"")
            }
            groupId = parts[0]
            artifactId = parts[1]
            if (parts.length > 2) {
                versionName = parts[2]
            }
        }

    }

    String getProject() {
        return project
    }

    void setProject(String project) {
        this.project = project
    }

    String getBuildScope() {
        return buildScope
    }

    void setBuildScope(String buildScope) {
        this.buildScope = buildScope
    }

    String getFlavorScope() {
        return flavorScope
    }

    void setFlavorScope(String flavorScope) {
        this.flavorScope = flavorScope
    }

    boolean getUseProject() {
        return useProject
    }

    void setUseProject(boolean useProject) {
        this.useProject = useProject
    }

    String getArtifactId() {
        return artifactId
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId
    }

    String getGroupId() {
        return groupId
    }

    void setGroupId(String groupId) {
        this.groupId = groupId
    }

    String getVersionName() {
        return versionName
    }

    void setVersionName(String versionName) {
        this.versionName = versionName
        this.remote = "$groupId:$artifactId:$versionName"
    }

    boolean getDisabled() {
        return disabled
    }

    void setDisabled(boolean disabled) {
        this.disabled = disabled
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Module module = (Module) o

        if (artifactId != module.artifactId) return false
        if (groupId != module.groupId) return false

        return true
    }

    int hashCode() {
        int result
        result = (artifactId != null ? artifactId.hashCode() : 0)
        result = 31 * result + (groupId != null ? groupId.hashCode() : 0)
        return result
    }


    @Override
    public String toString() {
        return "Module{" +
                "name='" + name + '\'' +
                ", remote='" + remote + '\'' +
                ", project='" + project + '\'' +
                ", useProject=" + useProject +
                ", disabled=" + disabled +
                '}';
    }
}
