package com.kheera.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtils {
    private FileUtils() {
    } //private constructor to enforce Singleton pattern

    public static boolean fileExists(String filePath) {
        final File file = new File(filePath);
        return file.exists();
    }

    public static boolean delete(String filePath) {
        if (fileExists(filePath)) {
            final File file = new File(filePath);
            return file.delete();
        }
        return false;
    }

    public static String readAllText(InputStream stream) throws IOException {
        final StringBuilder text = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null)
                br.close();
        }
    }

    public static String readAllText(String filename) throws IOException {
        File file = new File(filename);

        StringBuilder text = new StringBuilder();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null)
                br.close();
        }
    }

    public static void writeAllText(String filename, String content) throws IOException {
        File file = new File(filename);

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            bw.write(content);
        } catch (IOException e) {
            throw e;
        } finally {
            if (bw != null)
                bw.close();
        }
    }

    public static void createDirectory(String path) {
        File dir = new File(path);
        dir.mkdirs();
    }
}