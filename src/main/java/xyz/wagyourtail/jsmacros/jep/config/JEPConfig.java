package xyz.wagyourtail.jsmacros.jep.config;

import xyz.wagyourtail.jsmacros.core.config.Option;
import xyz.wagyourtail.jsmacros.core.config.OptionType;
import xyz.wagyourtail.jsmacros.jep.client.JsMacrosJEP;

public class JEPConfig {
    @Option(translationKey = "Shared Library Path", group = {"jsmacros.settings.languages", "jsmacros.settings.languages.jep"}, setter = "setPath", type = @OptionType(value = "file", options = "topLevel=MC"))
    public String path = "./jep.dll";
    
    public void setPath(String path) {
        this.path = path;
        JsMacrosJEP.addSharedLibrary(path);
    }
}
