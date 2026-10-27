# JD-GUI

[![CI](https://github.com/asprenas/jd-gui/actions/workflows/ci.yml/badge.svg)](https://github.com/asprenas/jd-gui/actions/workflows/ci.yml)

JD-GUI, a standalone graphical utility that displays Java sources from CLASS files.

> **Fork notice:** this repository is a fork of [java-decompiler/jd-gui](https://github.com/java-decompiler/jd-gui),
> based on commit `b3c1ced` (JD-GUI 1.6.6). Changes in this fork:
> - Gradle 9.1, builds with JDK 21+; bytecode targets Java 21 (Java 21+ required to run)
> - JD-Core 1.3.2 from the [asprenas/jd-core](https://github.com/asprenas/jd-core) fork
> - Tests migrated to JUnit 5, GitHub Actions CI (JDK 21 / 25) and Dependabot
> - `.java` files are parsed with a Java 24 grammar ([antlr/grammars-v4](https://github.com/antlr/grammars-v4/tree/master/java/java), ANTLR 4.13.2)
  instead of Java 7, so sources using lambdas, `var`, records, sealed types, switch expressions, text blocks and patterns
  including record types and their implicit members, method references, hyperlinks and indexing
> - RSyntaxTextArea 3.6.3: syntax highlighting of text blocks and of the `record`, `sealed`, `permits`, `yield` and `var` keywords
> - Long Java expressions are parsed on a dedicated large-stack thread to avoid `StackOverflowError`
> - Compact menus and shortened long paths in Recent Files; `.spring.components` opens as text

![](https://raw.githubusercontent.com/java-decompiler/jd-gui/master/src/website/img/jd-gui.png)

- Java Decompiler projects home page: [http://java-decompiler.github.io](http://java-decompiler.github.io)
- JD-GUI source code: [https://github.com/java-decompiler/jd-gui](https://github.com/java-decompiler/jd-gui)

## Description
JD-GUI is a standalone graphical utility that displays Java source codes of 
".class" files. You can browse the reconstructed source code with the JD-GUI
for instant access to methods and fields.

## Download
Prebuilt packages: [GitHub Releases](https://github.com/asprenas/jd-gui/releases)

## How to build JD-GUI ?
Requirements: JDK 21 or later (Gradle 9). The resulting bytecode targets Java 21.

JD-Core 1.3.2 is resolved from Maven local (`org.jd:jd-core`, install it from
[asprenas/jd-core](https://github.com/asprenas/jd-core)) or from JitPack with `-PjdCoreJitpack`.
```
> git clone https://github.com/asprenas/jd-gui.git
> cd jd-gui
> ./gradlew build -PjdCoreJitpack
```
generate :
- _"build/libs/jd-gui-x.y.z.jar"_
- _"build/libs/jd-gui-x.y.z-min.jar"_
- _"build/distributions/jd-gui-windows-x.y.z.zip"_
- _"build/distributions/jd-gui-osx-x.y.z.tar"_
- _"build/distributions/jd-gui_x.y.z-0_all.deb"_
- _"build/distributions/jd-gui-x.y.z-0.noarch.rpm"_

## How to launch JD-GUI ?
- Double-click on _"jd-gui-x.y.z.jar"_
- Double-click on _"jd-gui.exe"_ application from Windows
- Double-click on _"JD-GUI"_ application from Mac OSX
- Execute _"java -jar jd-gui-x.y.z.jar"_ or _"java -classpath jd-gui-x.y.z.jar org.jd.gui.App"_

## How to use JD-GUI ?
- Open a file with menu "File > Open File..."
- Open recent files with menu "File > Recent Files"
- Drag and drop files from your file explorer

### Font appearance
Open `Help > Preferences... > Viewer > Appearance` to choose separate font families and sizes for Java source, other text pages, and the application UI. The font list contains families available to the current Java runtime. If a saved font is unavailable, JD-GUI falls back to the current editor theme or Swing Look and Feel default. Text font changes also update open pages and line numbers; UI font changes are applied immediately. Character coverage depends on fonts installed on the host system; install a font that supports a script if it appears as missing-glyph boxes.

## How to extend JD-GUI ?
```
> ./gradlew idea 
```
generate Idea Intellij project
```
> ./gradlew eclipse
```
generate Eclipse project
```
> java -classpath jd-gui-x.y.z.jar;myextension1.jar;myextension2.jar org.jd.gui.App
```
launch JD-GUI with your extensions

## How to uninstall JD-GUI ?
- Java: Delete "jd-gui-x.y.z.jar" and "jd-gui.cfg".
- Mac OSX: Drag and drop "JD-GUI" application into the trash.
- Windows: Delete "jd-gui.exe" and "jd-gui.cfg".

## License
Released under the [GNU GPL v3](LICENSE).
