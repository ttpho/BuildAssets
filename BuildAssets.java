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

/**
 * How to use
 * <p>
 * Open file pubspec.yaml , and
 * # === Generated Code Start ===
 * # === Generated Code End ===
 * below line *uses-material-design: true*
 * <p>
 * Run
 * javac BuildAssets.java
 * java BuildAssets
 */
public class BuildAssets {

    private static final String BUILD_GN_TOKEN_START = "# === Generated Code Start ===";
    private static final String BUILD_GN_TOKEN_END = "# === Generated Code End ===";
    private static final String BUILD_GN_TOKEN = String.format("%s(.*)%s", BUILD_GN_TOKEN_START, BUILD_GN_TOKEN_END);
    private static final Pattern BUILD_GN_GEN_PATTERN = Pattern.compile(BUILD_GN_TOKEN, Pattern.DOTALL);
    private static final String ASSETS_FOLDER = "assets";
    private static final String PUBSPEC_FILE_NAME = "pubspec.yaml";
    private static String GN_TOKEN_NOT_FOUND = String.format("The pubspec.yaml is defined:\n%s\n%s",
            BUILD_GN_TOKEN_START, BUILD_GN_TOKEN_END);
    private static String ASSETS_FOLDER_NOT_FOUND = "Assets folder not found";
    private static String ASSETS_FOLDER_EMPTY_FILE = "Assets folder is empty";

    public static void main(String[] args) {
        final String contentAssets = readFolderAssetsContent();
        if (contentAssets == null) return;

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
        final String genCode = String.format("%s\n%s  %s", BUILD_GN_TOKEN_START, contentAssets, BUILD_GN_TOKEN_END);
        final String newConentFile = matcher.replaceFirst(genCode);
        try {
            writeFileWithString(PUBSPEC_FILE_NAME, newConentFile);
        } catch (IOException e) {
            System.err.format("Exception: %s%n", e);
        }
    }

    public static String readFileAsString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    public static void writeFileWithString(String fileName, String content) throws IOException {
        Files.write(Paths.get(fileName), content.getBytes());
    }

    private static String readFolderAssetsContent() {
        final String directoryProject = new File("").getAbsolutePath();
        System.out.println(directoryProject);
        final File folderAssets = new File(directoryProject, ASSETS_FOLDER);
        if (!folderAssets.exists()) {
            System.out.println(ASSETS_FOLDER_NOT_FOUND);
            return null;
        }

        final List<String> allFileInAssets = new ArrayList<>();
        search(folderAssets, allFileInAssets);

        if (allFileInAssets.isEmpty()) {
            System.out.println(ASSETS_FOLDER_EMPTY_FILE);
            return null;
        }

        final StringBuffer textAssets = new StringBuffer("  assets:\n");
        for (final String fileName : allFileInAssets) {
            final String nameOnPubSpec = "    - " + fileName.replace(directoryProject + "/", "") + "\n";
            textAssets.append(nameOnPubSpec);
        }
        return textAssets.toString();
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
}
