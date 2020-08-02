# JsMacros-JEP

This extension adds `python 3.8` support to [JsMacros](https://github.com/wagyourtail/JsMacros) `1.2.3+`

It is annoying to setup but if you go through the effort It will give you full cython compatibility with the newest version of python.
In order to use `JEP` you will need to install it using the guide for your OS on their [wiki](https://github.com/ninia/jep/wiki).

## For Windows:
1. make sure you put python and pip in your path when installing.
2. install the newest version of [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)
3. launch the developer command prompt, and run `pip install -U jep`.
4. copy the DLL from `Python38\Lib\site-packages\jep\jep.dll` to your minecraft directory
5. set the relative path for the dll in `.minecraft/config/jsMacros/jep-options.json` (default is `.minecraft/jep.dll`)
6. profit

if you go through the effort to set this up on linux/mac please Pull-Request the steps or message @wagyourtail on the JsMacros discord

# issues/notes

* like with `JavaScript` functions will auto coerce to Consumer's, unlike `Jython` where you have to wrap functions.
