package xyz.wagyourtail.jsmacros.jep.config;

import xyz.wagyourtail.jsmacros.core.config.Option;
import xyz.wagyourtail.jsmacros.core.config.OptionType;
import xyz.wagyourtail.jsmacros.jep.JEPExtension;

public class JEPConfig {
    @Option(translationKey = "jsmacrosjep.sharedpath", group = {"jsmacros.settings.languages", "jsmacrosjep.settings.languages.jep"}, setter = "setPath", type = @OptionType(value = "file", options = "topLevel=MC"))
    public String path = "./jep.dll";

    @Option(translationKey = "jsmacrosjep.sharedlibs", group = {"jsmacros.settings.languages", "jsmacrosjep.settings.languages.jep"})
    public String sharedLibs = "numpy";


    public void setPath(String path) {
        this.path = path;
        JEPExtension.addSharedLibrary(path);
    }
}
