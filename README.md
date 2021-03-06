Build Assets Flutter
=====
Use command line to add an assets section [pubspec.yaml](https://flutter.dev/docs/development/ui/assets-and-images) file, and gen code dynamic to manager all image and json files.

<img src="/imgs/3.png" />

Setup Project Flutter 
-------

1. Download and move this `BuildAssets.java` inito project

2. Open file pubspec.yam  

Add 2 lines 

```java
 # === Generated Code Start ===
 # === Generated Code End ===
```

below line `uses-material-design: true`
<img src="/imgs/1.png" />

3. Open terminal and run

```java
 javac BuildAssets.java
 java BuildAssets
```

<img src="/imgs/2.png" />

4. Open pubspec.yaml and enjoy

Gen code
-------

| CMD  | Output |
| ------------- | ------------- |
| java BuildAssets JSON <package>.<class_name>.dart  | with command: </br>% java BuildAssets JSON local.manager.reader_json.dart</br> to gen file reader_json.dart into folder lib/local/manager/, the file is included static class ReaderJson</br> <img src="/imgs/5.png" /> |
| java BuildAssets AssetImage <package>.<class_name>.dart  | with command: </br>% java BuildAssets AssetImage local.manager.all_assets.dart</br> to gen file all_assets.dart into folder lib/local/manager/, the file is included static class AllAssets</br> <img src="/imgs/4.png" />  |

Idea
-------
The idea from Chromium source code: 
BuildConfigGenerator.groovy gen code from build.gradle to crete BUILD.gn file.
https://chromium.googlesource.com/chromium/src/+/refs/heads/master/third_party/android_deps/buildSrc/src/main/groovy/BuildConfigGenerator.groovy


License
-------
MIT License
Copyright (c) 2020 Pho Tran

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
