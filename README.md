# JsMacros-JEP

This extension adds `python 3.8` support to [JsMacros](https://github.com/wagyourtail/JsMacros) `1.2.3+`

It is annoying to setup but if you go through the effort It will give you full cython compatibility with the newest version of python.
In order to use `JEP` you will need to install it using the guide for your OS on their [wiki](https://github.com/ninia/jep/wiki).

## For Windows:

1. Download the JEP extension jar from the releases section of this Github repository, or build it yourself
2. Move the jar to the `config\jsMacros\LanguageExtensions` subfolder of your Minecraft directory
3. Install the newest version of [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)
4. Launch the developer command prompt, and run `pip install -U jep`.
5. Copy the DLL from `Python38\Lib\site-packages\jep\jep.dll` to the root of your minecraft directory

# Issues/Notes

If you want to put jep.dll anywhere besides the root of your Minecraft directory, Set the relative path for the file in `.minecraft/config/jsMacros/options.json` at the `jep.path` key

## Python
* Make sure you put python and pip in your path when installing.
* To get your `site-packages` directory location, run: `python -m site --user-site`

## Visual C++ Build Tools
* Make sure to select *Desktop development with C++* when you install the build tools

## JavaWrapper
* language spec requires that only one thread can hold an instance of the language at a time, so this implementation uses a non-preemptive priority queue for the threads that call the resulting MethodWrapper. 
