package com.kheera.internal;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by andrewc on 4/07/2016.
 */
public class AssetUtils {

    public static String ReadAsset(Context context, String filename) throws AutomationRunnerException {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(filename);
            in = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            throw new AutomationRunnerException("Error opening asset: " + filename, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new AutomationRunnerException("Error closing asset: " + filename, e);
                }
            }
        }
    }

    public static String[] List(Context context, String path) throws IOException {
        return context.getAssets().list(path);
    }
}
