/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.common.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.xipki.common.util.IoUtil;
import org.xipki.common.util.StringUtil;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CanonicalizeCode {

    private final String baseDir;

    private final int baseDirLen;

    private CanonicalizeCode(String baseDir) {
        this.baseDir = baseDir.endsWith(File.separator) ? baseDir : baseDir + File.separator;
        this.baseDirLen = this.baseDir.length();
    }

    public static void main(final String[] args) {
        for (String arg : args) {
            try {
                String baseDir = arg;
                System.out.println("Canonicalize dir " + baseDir);
                CanonicalizeCode canonicalizer = new CanonicalizeCode(baseDir);
                canonicalizer.canonicalize();
                canonicalizer.checkWarnings();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void canonicalize() throws Exception {
        canonicalizeDir(new File(baseDir), true);
    }

    private void canonicalizeDir(final File dir, boolean root) throws Exception {
        if (!root) {
            // skip git submodules
            if (new File(dir, ".git").exists()) {
                return;
            }
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String filename = file.getName();
            if (file.isDirectory()) {
                if (!"target".equals(filename) && !"tbd".equals(filename)) {
                    canonicalizeDir(file, false);
                }
            } else {
                int idx = filename.lastIndexOf('.');
                String extension = (idx == -1) ? filename : filename.substring(idx + 1);
                extension = extension.toLowerCase();

                if ("java".equals(extension)) {
                    canonicalizeFile(file);
                }
            }
        }
    } // method canonicalizeDir

    private void canonicalizeFile(final File file) throws Exception {
        byte[] newLine = detectNewline(file);

        BufferedReader reader = new BufferedReader(new FileReader(file));

        ByteArrayOutputStream writer = new ByteArrayOutputStream();

        try {
            String line;
            boolean skip = true;
            boolean lastLineEmpty = false;
            boolean licenseTextAdded = false;
            boolean thirdparty = false;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                if (lineNumber == 0 && line.startsWith("// #THIRDPARTY#")) {
                    thirdparty = true;
                    skip = false;
                }
                lineNumber++;

                if (line.trim().startsWith("package ") || line.trim().startsWith("import ")) {
                    if (!licenseTextAdded) {
                        if (!thirdparty) {
                            writeLicenseHeader(writer, newLine);
                        }
                        licenseTextAdded = true;
                    }
                    skip = false;
                }

                if (skip) {
                    continue;
                }

                String canonicalizedLine = canonicalizeLine(line);
                boolean addThisLine = true;
                if (canonicalizedLine.isEmpty()) {
                    if (!lastLineEmpty) {
                        lastLineEmpty = true;
                    } else {
                        addThisLine = false;
                    }
                } else {
                    lastLineEmpty = false;
                }

                if (addThisLine) {
                    writeLine(writer, newLine, canonicalizedLine);
                }
            } // end while
        } finally {
            writer.close();
            reader.close();
        }

        byte[] oldBytes = IoUtil.read(file);
        byte[] newBytes = writer.toByteArray();
        if (!Arrays.equals(oldBytes, newBytes)) {
            File newFile = new File(file.getPath() + "-new");
            IoUtil.save(file, newBytes);
            newFile.renameTo(file);
            System.out.println(file.getPath().substring(baseDirLen));
        }
    } // method canonicalizeFile

    private void checkWarnings() throws Exception {
        checkWarningsInDir(new File(baseDir), true);
    }

    private void checkWarningsInDir(final File dir, boolean root) throws Exception {
        if (!root) {
            // skip git submodules
            if (new File(dir, ".git").exists()) {
                return;
            }
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equals("target")
                        && !file.getName().equals("tbd")) {
                    checkWarningsInDir(file, false);
                }

                continue;
            } else {
                String filename = file.getName();
                int idx = filename.lastIndexOf('.');
                String extension = (idx == -1) ? filename : filename.substring(idx + 1);
                extension = extension.toLowerCase();

                if ("java".equals(extension)) {
                    checkWarningsInFile(file);
                }
            }
        }
    } // method checkWarningsInDir

    private void checkWarningsInFile(final File file) throws Exception {
        if (file.getName().equals("package-info.java")) {
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));

        boolean authorsLineAvailable = false;
        boolean thirdparty = false;

        List<Integer> lineNumbers = new LinkedList<>();

        int lineNumber = 0;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("// #THIRDPARTY")) {
                    return;
                }

                if (!authorsLineAvailable && line.contains("* @author")) {
                    authorsLineAvailable = true;
                }

                if (line.length() > 100 && !line.contains("http")) {
                    lineNumbers.add(lineNumber);
                }
            } // end while
        } finally {
            reader.close();
        }

        if (!lineNumbers.isEmpty()) {
            System.out.println("Please check file " + file.getPath().substring(baseDirLen)
                + ": lines " + Arrays.toString(lineNumbers.toArray(new Integer[0])));
        }

        if (!authorsLineAvailable && !thirdparty) {
            System.out.println("Please check file " + file.getPath().substring(baseDirLen)
                    + ": no authors line");
        }
    } // method checkWarningsInFile

    /**
     * replace tab by 4 spaces, delete white spaces at the end.
     */
    private static String canonicalizeLine(final String line) {
        if (line.trim().startsWith("//")) {
            // comments
            String nline = line.replace("\t", "    ");
            return removeTrailingSpaces(nline);
        }

        StringBuilder sb = new StringBuilder();
        int len = line.length();

        int lastNonSpaceCharIndex = 0;
        int index = 0;
        for (int i = 0; i < len; i++) {
            char ch = line.charAt(i);
            if (ch == '\t') {
                sb.append("    ");
                index += 4;
            } else if (ch == ' ') {
                sb.append(ch);
                index++;
            } else {
                sb.append(ch);
                index++;
                lastNonSpaceCharIndex = index;
            }
        }

        int numSpacesAtEnd = sb.length() - lastNonSpaceCharIndex;
        if (numSpacesAtEnd > 0) {
            sb.delete(lastNonSpaceCharIndex, sb.length());
        }

        String ret = sb.toString();
        if (ret.startsWith("    throws ")) {
            ret = "        " + ret;
        } else if (ret.startsWith("        throws ")) {
            ret = "    " + ret;
        }

        return ret;
    } // end canonicalizeLine

    private static String removeTrailingSpaces(final String line) {
        final int n = line.length();
        int idx;
        for (idx = n - 1; idx >= 0; idx--) {
            char ch = line.charAt(idx);
            if (ch != ' ') {
                break;
            }
        }
        return (idx == n - 1) ?  line : line.substring(0, idx + 1);
    } // method removeTrailingSpaces

    private static byte[] detectNewline(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte[] bytes = new byte[200];
        int size;
        try {
            size = is.read(bytes);
        } finally {
            is.close();
        }

        for (int i = 0; i < size - 1; i++) {
            byte bb = bytes[i];
            if (bb == '\n') {
                return new byte[]{'\n'};
            } else if (bb == '\r') {
                if (bytes[i + 1] == '\n') {
                    return new byte[]{'\r', '\n'};
                } else {
                    return new byte[]{'\r'};
                }
            }
        }

        return new byte[]{'\n'};
    }

    private static void writeLicenseHeader(OutputStream out, byte[] newLine) throws IOException {
        writeLine(out, newLine,
                "/*");
            writeLine(out, newLine,
                " *");
            writeLine(out, newLine,
                " * Copyright (c) 2013 - 2018 Lijun Liao");
            writeLine(out, newLine,
                " *");
            writeLine(out, newLine,
                " * Licensed under the Apache License, Version 2.0 (the \"License\");");
            writeLine(out, newLine,
                " * you may not use this file except in compliance with the License.");
            writeLine(out,newLine,
                " * You may obtain a copy of the License at");
            writeLine(out, newLine,
                " *");
            writeLine(out, newLine,
                " * http://www.apache.org/licenses/LICENSE-2.0");
            writeLine(out, newLine,
                    " *");
            writeLine(out, newLine,
                " * Unless required by applicable law or agreed to in writing, software");
            writeLine(out, newLine,
                " * distributed under the License is distributed on an \"AS IS\" BASIS,");
            writeLine(out, newLine,
                " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
            writeLine(out, newLine,
                " * See the License for the specific language governing permissions and");
            writeLine(out, newLine,
                " * limitations under the License.");
            writeLine(out, newLine,
                " */");
            writeLine(out, newLine,
                "");
    }

    private static void writeLine(OutputStream out, byte[] newLine, String line)
            throws IOException {
        if (StringUtil.isNotBlank(line)) {
            out.write(line.getBytes());
        }
        out.write(newLine);
    }

}
