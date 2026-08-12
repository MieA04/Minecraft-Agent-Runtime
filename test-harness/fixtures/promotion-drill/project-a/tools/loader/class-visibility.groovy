/*
MAR Tool
Path: loader/class-visibility.groovy
Status: project-verified
Minecraft: any MAR-supported version
Loader: context ClassLoader fixture
Depends-On-Project-Code: false
Evidence:
- knowledge/discoveries/20260812-001-class-visibility.md
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
