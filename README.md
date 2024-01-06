# JsMacros-JEP

This extension adds `python 3.8` support to [JsMacros](https://github.com/wagyourtail/JsMacros) `1.2.3+`

It is annoying to setup but if you go through the effort It will give you full cython compatibility with the newest version of python.
In order to use `JEP` you will need to install it using the guide for your OS on their [wiki](https://github.com/ninia/jep/wiki).

# How To Install JEP

## Platform Independent Steps (Do before each platform)

1. Download the JEP extension jar from the releases section of this Github repository, or build it yourself
2. Move the jar to the `config\jsMacros\LanguageExtensions` subfolder of your Minecraft directory

## For MacOS (probably also linux)
#### this tutorial is slightly technical, but if you know how to code; you are probably fine
##### Requirements: Python 3.8+ is installed, Knowlege of how to use a terminal
##### In some of the paths in this tutorial, i have used an `X` to represent the part of the python version, this will defer depending on what version of python you use, i have been using python 3.12
1. Create and enter a virtual enviroment somewhere in your minecraft instance root (.minecraft folder) <br> execute `$ python3 -m venv .` in your instance root to make your instance root the virtual enviroment, then to activate it if it is in your instance root, use `$ ./bin/activate`
2. do `python3 -m pip install jep`
3. direct jsmacros to the jep executeable, it is in the `./lib/python3.X/jep/` folder, it should be named something like `jep.cpython-3X-darwin.so` on macos<br>this can be done in the JEP tab of the jsmacros settings. it will not imediately replace the path in the field, you have to close and reopen the settings ui first

## For Windows:

1. Install the newest version of [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)
2. Launch the developer command prompt, and run `pip install -U jep`.
3. Copy the DLL from `Python38\Lib\site-packages\jep\jep.dll` to the root of your minecraft directory

# Issues/Notes

If you want to put jep.dll anywhere besides the root of your Minecraft directory, Set the relative path for the file in `.minecraft/config/jsMacros/options.json` at the `jep.path` key

## Python
* Make sure you put python and pip in your path when installing.
* To get your `site-packages` directory location, run: `python -m site --user-site`

## Visual C++ Build Tools
* Make sure to select *Desktop development with C++* when you install the build tools

## JavaWrapper
* language spec requires that only one thread can hold an instance of the language at a time, so this implementation uses a non-preemptive priority queue for the threads that call the resulting MethodWrapper. 
