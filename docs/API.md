The Velocitab API provides methods for vanishing ("hiding") and modifying usernames on the TAB list.

The API is distributed on Maven through [repo.william278.net](https://repo.william278.net/#/releases/net/william278/velocitab/) and can be included in any Maven, Gradle, etc. project. JavaDocs are [available here](https://repo.william278.net/javadoc/releases/net/william278/velocitab/latest).

Velocitab also provides a plugin message API, which is documented in the [[Plugin Message API Examples]] page.

## Compatibility
[![Maven](https://repo.william278.net/api/badge/latest/releases/net/william278/velocitab?color=00fb9a&name=Maven&prefix=v)](https://repo.william278.net/#/releases/net/william278/velocitab/)

The Velocitab API shares version numbering with the plugin itself for consistency and convenience. Please note minor and patch plugin releases may make API additions and deprecations, but will not introduce breaking changes without notice.

| API Version |   Velocitab Versions   | Supported |
|:-----------:|:----------------------:|:---------:|
|    v1.x     | _v1.5.2&mdash;Current_ |     ✅     |

## Table of contents
1. Adding the API to your project
2. Adding Velocitab as a dependency
3. Next steps

## API Introduction
### 1.1 Setup with Maven
<details>
<summary>Maven setup information</summary>

Add the repository to your `pom.xml` as per below. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```xml
<repositories>
    <repository>
        <id>william278.net</id>
        <url>https://repo.william278.net/releases</url>
    </repository>
</repositories>
```
Add the dependency to your `pom.xml` as per below. Replace `VERSION` with the latest version of Velocitab (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/Velocitab?color=%23282828&label=%20&style=flat-square)
```xml
<dependency>
    <groupId>net.william278</groupId>
    <artifactId>velocitab</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```
</details>

### 1.2 Setup with Gradle
<details>
<summary>Gradle setup information</summary>

Add the dependency as per below to your `build.gradle`. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```groovy
allprojects {
	repositories {
		maven { url 'https://repo.william278.net/releases' }
	}
}
```
Add the dependency as per below. Replace `VERSION` with the latest version of Velocitab (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/Velocitab?color=%23282828&label=%20&style=flat-square)

```groovy
dependencies {
    compileOnly 'net.william278:velocitab:VERSION'
}
```
</details>

### 2. Adding Velocitab as a dependency
Add Velocitab as a dependency in your main class annotation:

```java
@Plugin(
  id = "myplugin",
  name = "My Plugin",
  version = "0.1.0",
  dependencies = {
    @Dependency(id = "velocitab", optional = true)
  }
)
public class MyPlugin {
  // ...
}
```

<details>
<summary>Alternative method: Adding to `velocity-plugin.json`</summary>

```json
{
  "dependencies": [
    {
      "id": "velocitab",
      "optional": true
    }
  ]
}
```
</details>

## 3. Creating a class to interface with the API
- Unless your plugin completely relies on Velocitab, you shouldn't put Velocitab API calls into your main class, otherwise if Velocitab is not installed you'll encounter `ClassNotFoundException`s

```java
public class VelocitabAPIHook {

    public VelocitabAPIHook() {
        // Ready to do stuff with the API
    }

}
```
## 4. Checking if Velocitab is present and creating the hook
- Check to make sure the Velocitab plugin is present before instantiating the API hook class

```java

@Plugin(
        id = "myplugin",
        name = "My Plugin",
        version = "0.1.0",
        dependencies = {
                @Dependency(id = "velocitab", optional = true)
        }
)
public class MyPlugin {

    public VelocitabAPIHook velocitabHook;

    @Subscribe
    public void onProxyInitialization(@NotNull ProxyInitializeEvent event) {
        if (event.getProxy().getPluginManager().getPlugin("velocitab").isPresent()) {
            velocitabHook = new VelocitabAPIHook();
        }
    }

}
```

## 5. Getting an instance of the API
- You can now get the API instance by calling `VelocitabAPI#getInstance()`

```java
import net.william278.velocitab.api.BukkitVelocitabAPI;

public class VelocitabAPIHook {

    private final VelocitabAPI velocitabApi;

    public VelocitabAPIHook() {
        this.velocitabApi = VelocitabAPI.getInstance();
    }

}
```

### 6. Next steps
Now that you've got everything ready, you can start doing stuff with the Velocitab API!
- [[API Examples]]