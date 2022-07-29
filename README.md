# JsMacros-JEP

This extension adds `python 3.8` support to [JsMacros](https://github.com/wagyourtail/JsMacros) `1.2.3+`

It is annoying to setup but if you go through the effort It will give you full cython compatibility with the newest version of python.
In order to use `JEP` you will need to install it using the guide for your OS on their [wiki](https://github.com/ninia/jep/wiki).

## For Windows:
1. make sure you put python and pip in your path when installing.
2. install the newest version of [Visual C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)
3. launch the developer command prompt, and run `pip install -U jep`.
4. copy the DLL from `Python38\Lib\site-packages\jep\jep.dll` to your minecraft directory
5. Set the relative path for the `dll` in `.minecraft/config/jsMacros/options.json` at the `jep.path` key (default is `.minecraft/jep.dll`)

# issues/notes

* Use the consumer wrappers to make JEP able to run consumers properly.
* JEP expects each consumer to be run at least once and after they have all been run, will close, make sure to add a consumer to onClose (or in `draw2d` re-define the consumer for init at the bottom of the init function). you can also close the JEP instance early with `consumer.stop()`.


## 1.0.1+

### toConsumer(function) *1.0.1+*
*Example:* `consumer.toConsumer(func)`

### toBiConsumer(function) *1.0.1+*
*Example:* `consumer.toBiConsumer(func)`

### stop() *1.0.1+*
*Example:* `consumer.stop()`

