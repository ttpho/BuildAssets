/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Pho Tran
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * How to use
 * % java BuildAssets
 * -> add add an assets section
 * % java BuildAssets JSON <package>.<class_name>.dart
 * -> with command: % java BuildAssets JSON local.manager.reader_json.dart
 * to gen file reader_json.dart into folder lib/local/manager/, the file is included static class ReaderJson
 * % java BuildAssets AssetImage <package>.<class_name>.dart
 * -> with command: % java BuildAssets AssetImage local.manager.all_assets.dart
 * to gen file all_assets.dart into folder lib/local/manager/, the file is included static class AllAssets
 */
public class BuildAssets {

    private static final String BUILD_GN_TOKEN_START = "# === Generated Code Start ===";
    private static final String BUILD_GN_TOKEN_END = "# === Generated Code End ===";
    private static final String BUILD_GN_TOKEN = String.format("%s(.*)%s", BUILD_GN_TOKEN_START, BUILD_GN_TOKEN_END);
    private static final Pattern BUILD_GN_GEN_PATTERN = Pattern.compile(BUILD_GN_TOKEN, Pattern.DOTALL);

    private static final String ASSETS_FOLDER = "assets";
    private static final String ASSETS_FOLDER_NOT_FOUND = "Assets folder not found";
    private static final String ASSETS_FOLDER_EMPTY_FILE = "Assets folder is empty";
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss:SSS";
    private static final String START = "START ! ";
    private static final String FINISH = "FINISH! ";


    private static final String DEFINE_ASSETS_LINE = "  assets:\n";
    private static final String PUBSPEC_FILE_NAME = "pubspec.yaml";
    private static final String ONE_LINE_RES_PATH_FORMAT = "    - %s\n";
    private static final String GN_TOKEN_NOT_FOUND = String.format("The pubspec.yaml must be defined:\n%s\n%s",
            BUILD_GN_TOKEN_START, BUILD_GN_TOKEN_END);

    private static final String[] SUPPOTED_IMAGE_FORMATS = {
            "bmp",
            "exr",
            "gif",
            "jpeg",
            "png",
            "psd",
            "pvrtc",
            "tga",
            "tiff",
            "webp",
            "svg"
    };
    private static final String ARG_GEN_CODE_ASSET_IMAGE = "AssetImage";
    private static final String ARG_GEN_CODE_JSON = "JSON";
    private static final String DART_FILE = ".dart";
    private static final String TEMP_DART_FILE = "*dart";
    private static final String GEN_CODE_STATIC = "  static final %s = %s";
    private static final String IMAGE_FILE_PATH_CODE_FORMAT = "'%s';";

    private static final String BUILD_DART_TOKEN_START = "// === Generated Code Start ===";
    private static final String BUILD_DART_TOKEN_END = "// === Generated Code End ===";
    private static final String BUILD_DART_TOKEN = String.format("%s(.*)%s", BUILD_DART_TOKEN_START, BUILD_DART_TOKEN_END);
    private static final Pattern BUILD_DART_GEN_PATTERN = Pattern.compile(BUILD_DART_TOKEN, Pattern.DOTALL);
    private static final String DART_TOKEN_NOT_FOUND = String.format("The class file is defined:\n%s\n%s",
            BUILD_DART_TOKEN_START, BUILD_DART_TOKEN_END);
    private static final String PATH_LIB_FORMAT = "lib/%s";

    private static final String GUIDE = "Please read description content script via\nhttps://github.com/ttpho/BuildAssets";

    public static void main(String[] args) {

        final AssetsResult assetsResult = readFolderAssetsContent();
        if (assetsResult == null) return;

        if (args.length == 0) {
            PubspecYaml.updatePubspecYamlFile(assetsResult);
            return;
        }

        if (args.length == 1) {
            System.out.println(GUIDE);
            return;
        }
        if (args.length == 2) {
            final String arg = args[1].trim();
            if (!arg.endsWith(DART_FILE)) {
                System.out.println(GUIDE);
                return;
            }
            final String[] splitArg = arg.split("\\.");
            final int splitArgLength = splitArg.length;
            final String className = formatClassName(splitArg[splitArgLength - 2]);

            if (ARG_GEN_CODE_ASSET_IMAGE.equals(args[0])) {
                DartFileCreater.genDartFile(
                        arg,
                        AssetImage.createTemplateContent(className, assetsResult.imageObjects),
                        AssetImage.createAssetImageDartCode(assetsResult.imageObjects)
                );
                return;
            }

            if (ARG_GEN_CODE_JSON.equals(args[0])) {
                DartFileCreater.genDartFile(
                        arg,
                        JsonFile.createTemplateContent(className, assetsResult.jsonItemList),
                        JsonFile.createJsonDartCode(assetsResult.jsonItemList)
                );
                return;
            }

            System.out.println(GUIDE);
            return;
        }
    }

    public static String readFileAsString(String fileName) throws IOException {
        return readFileAsString(Paths.get(fileName));
    }

    public static String readFileAsString(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    public static void writeFileWithString(Path path, String content) throws IOException {
        Files.write(path, content.getBytes());
    }

    public static void writeFileWithString(String fileName, String content) throws IOException {
        writeFileWithString(Paths.get(fileName), content);
    }

    private static final List<String> allFileInAssets() {
        final String directoryProject = new File("").getAbsolutePath();
        final File folderAssets = new File(directoryProject, ASSETS_FOLDER);
        if (!folderAssets.exists()) {
            System.out.println(ASSETS_FOLDER_NOT_FOUND);
            return null;
        }

        final List<String> allFileInAssets = new ArrayList<>();
        search(folderAssets, allFileInAssets);
        return allFileInAssets;
    }

    private static AssetsResult readFolderAssetsContent() {
        final String directoryProject = new File("").getAbsolutePath();
        final List<String> allFileInAssets = allFileInAssets();
        if (allFileInAssets == null || allFileInAssets.isEmpty()) {
            System.out.println(ASSETS_FOLDER_EMPTY_FILE);
            return null;
        }

        final List<ImageObject> imageObjects = new ArrayList();
        final List<JsonItem> jsonItemList = new ArrayList();
        final StringBuffer textAssets = new StringBuffer(DEFINE_ASSETS_LINE);

        for (final String filePath : allFileInAssets) {
            if (filePath == null || filePath.isEmpty()) continue;

            final String filePathWithoutDirectoryProject = filePath.replace(directoryProject + "/", "");
            final String fileNameWithExtension = getFileName(filePathWithoutDirectoryProject);

            if (AssetImage.isSupportedImageFormats(filePath)) {
                final ImageObject item = new ImageObject();
                final String name = formatName(fileNameWithExtension);
                final String defineAssetImage = String.format(IMAGE_FILE_PATH_CODE_FORMAT, filePathWithoutDirectoryProject);
                item.line = String.format(GEN_CODE_STATIC, name.replaceAll("-", "_"), defineAssetImage);
                item.fileName = fileNameWithExtension;
                imageObjects.add(item);
            } else if (JsonFile.isJsonFile(filePath)) {
                final JsonItem jsonItem = new JsonItem();
                jsonItem.filePath = filePathWithoutDirectoryProject;
                final String name = formatName(fileNameWithExtension);
                jsonItem.methodName = JsonFile.makeMethodName(name);
                jsonItemList.add(jsonItem);
            }

            final String oneLineOnSpec = String.format(ONE_LINE_RES_PATH_FORMAT, filePathWithoutDirectoryProject);
            textAssets.append(oneLineOnSpec);
        }

        final AssetsResult assetsResult = new AssetsResult();
        assetsResult.textAssets = textAssets.toString();
        assetsResult.imageObjects = imageObjects;
        assetsResult.jsonItemList = jsonItemList;
        return assetsResult;
    }

    private static String getFileName(String filePathWithoutDirectoryProject) {
        try {
            final String[] splitFileName = filePathWithoutDirectoryProject.split("/");
            final String nameWithExtension = splitFileName[splitFileName.length - 1].trim();
            return nameWithExtension;
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatName(String name) {
        final String[] splitname = name.split("_");
        if (splitname.length == 1) {
            return name.split("\\.")[0];
        }
        String newName = splitname[0];
        for (int i = 1; i < splitname.length; i++) {
            final String sec = splitname[i];
            final char[] nameChars = sec.toCharArray();
            nameChars[0] = Character.toUpperCase(sec.charAt(0));
            newName = newName + String.valueOf(nameChars);
        }
        return newName.split("\\.")[0];
    }

    private static String formatClassName(final String className) {
        final String name = "_" + className;
        final String[] splitname = name.split("_");
        if (splitname.length == 1) return name;
        String newName = splitname[0];
        for (int i = 1; i < splitname.length; i++) {
            final String sec = splitname[i];
            final char[] nameChars = sec.toCharArray();
            nameChars[0] = Character.toUpperCase(sec.charAt(0));
            newName = newName + String.valueOf(nameChars);
        }
        return newName;
    }

    public static void search(final File folder, List<String> result) {
        for (final File f : folder.listFiles()) {
            if (f.isDirectory()) {
                search(f, result);
            }
            if (f.isFile() && !isIgnoreFile(f)) {
                result.add(f.getAbsolutePath());
            }
        }
    }

    private static boolean isIgnoreFile(File file) {
        return file.getName().equals(".DS_Store");
    }

    private static String curentTime() {
        try {
            return (new SimpleDateFormat(DATE_FORMAT)).format(new Date(System.currentTimeMillis()));
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String getLocalFilePath(String arg) {
        final String tempDartFile = arg.replace(DART_FILE, TEMP_DART_FILE);
        final String tempPath = tempDartFile.replace(".", "/");
        final String pathName = tempPath.replace(TEMP_DART_FILE, DART_FILE);

        return pathName;
    }

    public static void createNewDartFile(final Path path, final String fullContent) throws Exception {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
        writeFileWithString(path, fullContent);
    }

    public static void updateDartFile(final Path path, final String updateContent) {
        String classContent = "";
        try {
            classContent = readFileAsString(path);
        } catch (IOException readException) {
            System.err.format("Exception: %s%n", readException);
            return;
        }
        final Matcher matcher = BUILD_DART_GEN_PATTERN.matcher(classContent);
        if (!matcher.find()) {
            System.out.println(DART_TOKEN_NOT_FOUND);
            return;
        }
        final String genCode = String.format("%s\n\n%s\n%s", BUILD_DART_TOKEN_START, updateContent, BUILD_DART_TOKEN_END);
        final String newConentFile = matcher.replaceFirst(genCode);
        try {
            writeFileWithString(path, newConentFile);
        } catch (IOException writeException) {
            System.err.format("Exception: %s%n", writeException);
        }
    }


    private static class PubspecYaml {
        public static void updatePubspecYamlFile(final AssetsResult assetsResult) {
            String pubspecFileContent = "";
            try {
                pubspecFileContent = readFileAsString(PUBSPEC_FILE_NAME);
            } catch (IOException e) {
                System.err.format("Exception: %s%n", e);
                return;
            }

            final Matcher matcher = BUILD_GN_GEN_PATTERN.matcher(pubspecFileContent);
            if (!matcher.find()) {
                System.out.println(GN_TOKEN_NOT_FOUND);
                return;
            }
            final String genCode = String.format("%s\n%s  %s", BUILD_GN_TOKEN_START, assetsResult.textAssets, BUILD_GN_TOKEN_END);
            final String newConentFile = matcher.replaceFirst(genCode);
            try {
                writeFileWithString(PUBSPEC_FILE_NAME, newConentFile);
            } catch (IOException e) {
                System.err.format("Exception: %s%n", e);
            }
        }
    }

    private static class ImageObject {
        public String fileName;
        public String line;
    }

    private static class JsonItem {
        public String filePath;
        public String methodName;

        public String createMethodFunction() {
            if (filePath == null || methodName == null) return "";

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("  static Future<String> METHOD_NAME() async {".replace("METHOD_NAME", methodName));
            printWriter.println("    return await rootBundle.loadString('FILE_PATH');".replace("FILE_PATH", filePath));
            printWriter.println("  }");

            return stringWriter.toString();
        }
    }

    private static class AssetsResult {
        public String textAssets;
        public List<ImageObject> imageObjects;
        public List<JsonItem> jsonItemList;
    }

    private static class DartFileCreater {
        private static void createDartFile(final String fileFullPath,
                                           final String fullContent,
                                           final String updateContent) {
            final Path path = Paths.get(fileFullPath);
            try {
                createNewDartFile(path, fullContent);
            } catch (Exception e) {
                if (e instanceof FileAlreadyExistsException) {
                    updateDartFile(path, updateContent);
                    return;
                }
                System.err.format("Exception: %s%n", e);
            }
        }

        public static void genDartFile(final String arg,
                                       final String fullContent,
                                       final String updateContent) {
            final String[] splitArg = arg.split("\\.");
            final int splitArgLength = splitArg.length;
            final String fileName = splitArg[splitArgLength - 2] + DART_FILE;
            final boolean hasPackage = splitArgLength > 2;

            final String filePath = hasPackage ? getLocalFilePath(arg) : fileName;
            final String fileFullPath = String.format(PATH_LIB_FORMAT, filePath);
            final String className = formatClassName(splitArg[splitArgLength - 2]);
            createDartFile(fileFullPath, fullContent, updateContent);
        }
    }

    private static class AssetImage {
        public static boolean isSupportedImageFormats(final String fileName) {
            final String fileNameLowerCase = fileName.toLowerCase();
            for (final String format : SUPPOTED_IMAGE_FORMATS) {
                if (fileNameLowerCase.endsWith("." + format)) {
                    return true;
                }
            }
            return false;
        }

        public static String createTemplateContent(final String className,
                                                   final List<ImageObject> imageObjects) {
            if (className == null || className.isEmpty()) return "";
            if (imageObjects == null || imageObjects.isEmpty()) return "";

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("");
            //start
            printWriter.println("class CLASS_NAME {".replace("CLASS_NAME", className));
            printWriter.println(BUILD_DART_TOKEN_START);
            printWriter.println("");
            printWriter.println(createAssetImageDartCode(imageObjects));
            printWriter.println(BUILD_DART_TOKEN_END);
            printWriter.println("}");
            printWriter.println("");
            return stringWriter.toString();
        }

        public static String createAssetImageDartCode(final List<ImageObject> imageObjects) {
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);

            for (final ImageObject item : imageObjects) {
                printWriter.println(item.line);
            }

            return stringWriter.toString();
        }
    }

    private static class JsonFile {
        private static final String METHOD_NAME_LOAD_JSON_FORAMT = "load%s";

        public static boolean isJsonFile(String fileName) {
            return fileName.toLowerCase().endsWith(".json");
        }

        public static String makeMethodName(final String name) {
            final char[] nameChars = name.toCharArray();
            nameChars[0] = Character.toUpperCase(name.charAt(0));
            return String.format(METHOD_NAME_LOAD_JSON_FORAMT, String.valueOf(nameChars));
        }

        public static String createTemplateContent(final String className, final List<JsonItem> jsonItemList) {
            if (className == null || className.isEmpty()) return "";
            if (jsonItemList == null || jsonItemList.isEmpty()) return "";

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("import 'dart:async' show Future;");
            printWriter.println("");
            printWriter.println("import 'package:flutter/services.dart' show rootBundle;");
            printWriter.println("");

            printWriter.println("class CLASS_NAME {".replace("CLASS_NAME", className));
            printWriter.println(BUILD_DART_TOKEN_START);
            printWriter.println("");
            printWriter.println(createJsonDartCode(jsonItemList));
            printWriter.println(BUILD_DART_TOKEN_END);
            printWriter.println("}");
            printWriter.println("");
            return stringWriter.toString();
        }

        private static String createJsonDartCode(final List<JsonItem> jsonItemList) {
            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);

            final int size = jsonItemList.size();
            for (int i = 0; i < size; i++) {
                final JsonItem item = jsonItemList.get(i);
                if (i == (size - 1)) {
                    printWriter.print(item.createMethodFunction());
                } else {
                    printWriter.println(item.createMethodFunction());
                }
            }

            return stringWriter.toString();
        }
    }
}