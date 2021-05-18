package chao.android.gradle.plugin.dependencies;

import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.initialization.DefaultSettings;
import org.gradle.invocation.DefaultGradle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import chao.android.gradle.plugin.api.SettingsInject;
import chao.android.gradle.plugin.base.PluginException;
import chao.android.gradle.plugin.util.StringUtils;

public class ModuleHandler {

    private DefaultSettings settings;

    private DefaultGradle gradle;

    private final Map<String, ModuleBuilder> builders;

    String moduleParent;

    private final List<ModuleHandler> childHandlers;

    public ModuleHandler() {
        builders = new HashMap<>();
        childHandlers = new ArrayList<>();
        moduleParent = "";
    }

    public void clearCache() {
        builders.clear();
        for (ModuleHandler handler: childHandlers) {
            handler.clearCache();
        }
        childHandlers.clear();
    }

    public void setSettings(DefaultSettings settings) {
        this.settings = settings;
    }

    public DefaultSettings getSettings() {
        return this.settings;
    }

    public void setGradle(DefaultGradle gradle) {
        this.gradle = gradle;
    }

    public ProjectDescriptor getRootProject() {
        return settings.getRootProject();
    }


    public void include(String... projectPaths) {
        settings.include(projectPaths);
    }

    public void includeFlat(String... projectNames) {
        settings.includeFlat(projectNames);
    }

    public ProjectDescriptor project(String path) {
        return settings.project(path);
    }

    public ProjectDescriptor project(File file) {
        return settings.project(file);
    }


    public ModuleBuilder module(String moduleName, String remoteName, String projectName) {
        checkModuleName(moduleName);
        if (StringUtils.isEmpty(moduleName)) {
            throw new PluginException("invilid module: ${moduleName} -> ${remoteName} -> ${projectName}" );
        }
        ModuleBuilder moduleBuilder = new ModuleBuilder(this);
        moduleBuilder.name(moduleName).remote(remoteName).project(projectName);
        if (StringUtils.isEmpty(remoteName)) {
            project(moduleName, projectName);
        } else if (StringUtils.isEmpty(projectName)) {
            remote(moduleName, remoteName);
        }
        builders.put(moduleName, moduleBuilder);
        return moduleBuilder;
    }

    public ModuleBuilder project(String moduleName, String project) {
        checkModuleName(moduleName);
        if (StringUtils.isEmpty(project)) {
            throw new PluginException("invilid module: ${moduleName} -> ${project}" );
        }
        ModuleBuilder moduleBuilder = builders.get(moduleName);
        if (moduleBuilder == null) {
            moduleBuilder = new ModuleBuilder(this);
            builders.put(moduleName, moduleBuilder);
        }
        moduleBuilder.name(moduleName).project(project);
        return moduleBuilder;
    }

    public ModuleBuilder module(String moduleName) {
        checkModuleName(moduleName);
        ModuleBuilder moduleBuilder = builders.get(moduleName);
        if (moduleBuilder == null) {
            for (ModuleHandler child: childHandlers) {
                moduleBuilder = child.builders.get(moduleName);
                if (moduleBuilder != null) {
                    return moduleBuilder;
                }
            }
        }
        if (moduleBuilder == null) {
            moduleBuilder = new ModuleBuilder(this);
            moduleBuilder.name(moduleName);
            builders.put(moduleName, moduleBuilder);
        }
        return moduleBuilder;
    }

//    ModuleBuilder module(String moduleName, String remoteName) {
//        checkModuleName(moduleName);
//        remote(moduleName, remoteName);
//    }

    public ModuleBuilder remote(String moduleName, String remoteName) {
        checkModuleName(moduleName);
        if (StringUtils.isEmpty(remoteName)) {
            throw new PluginException("invalid module: ${moduleName} -> ${remoteName}" );
        }
        ModuleBuilder moduleBuilder = builders.get(moduleName);
        if (moduleBuilder == null) {
            moduleBuilder = new ModuleBuilder(this);
            builders.put(moduleName, moduleBuilder);
        }
        moduleBuilder.name(moduleName).remote(remoteName);
        return moduleBuilder;
    }

    public void setModuleParent(String path) {
        moduleParent = path;
    }

    /**
     * @param path 相对当前modules.gradle的路径
     */
    public void subcontainer(String path) {
        if (StringUtils.isEmpty(path)) {
            return;
        }
        ModuleHandler childHandler = new ModuleHandler();
        childHandlers.add(childHandler);
        childHandler.setSettings(settings);
        childHandler.setModuleParent(moduleParent + path);
        childHandler.setGradle(gradle);
        String parentDir = childHandler.moduleParent.substring(1);//去掉第一个":"
        File childScriptFile = new File(settings.getRootDir() + File.separator + parentDir + File.separator + SettingsInject.DEFAULT_MODULES_SETTINGS_FILE);
        ScriptHelper.applyModuleScript(Collections.singletonList(childScriptFile), gradle, settings, childHandler);
    }


    private static void checkModuleName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new RuntimeException("module name should not be empty.");
        }
        if (!name.matches("^[a-zA-Z0-9_]*$")) {
            throw new RuntimeException("module name only contains (0-9, a-z, A-Z and _) characters"); ///字符
        }
    }

    List<Module> allModules() {
        Stream<Module> stream = new ArrayList<>(builders.values()).stream().map(ModuleBuilder::build);
        for (ModuleHandler handler: childHandlers) {
            stream = Stream.concat(stream, handler.allModules().stream());
        }
        return stream.collect(Collectors.toList());
    }
}
