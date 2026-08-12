/*
MAR Public Tool
Scope: loader
Status: public-verified
Minecraft: any MAR-supported version
Loader: any environment exposing the active Mod ClassLoader
Project Dependencies: none
Evidence: references/public-loader-tools.md
*/

return [
    resolve: { String className ->
        Class.forName(className, false, Thread.currentThread().contextClassLoader)
    },
    isPresent: { String className ->
        try {
            Class.forName(className, false, Thread.currentThread().contextClassLoader)
            true
        } catch (ClassNotFoundException | LinkageError ignored) {
            false
        }
    }
]
