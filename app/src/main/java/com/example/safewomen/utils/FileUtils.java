package com.example.safewomen.utils;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;

/**
 * Utility class for file operations
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * Scan a file to make it visible in the media gallery
     * @param context Application context
     * @param filePath Path to the file to scan
     */
    public static void scanFile(Context context, String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                MediaScannerConnection.scanFile(
                        context,
                        new String[]{filePath},
                        null,
                        (path, uri) -> Log.d(TAG, "File scanned: " + path + " -> " + uri)
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning file: " + filePath, e);
        }
    }

    /**
     * Delete a file
     * @param filePath Path to the file to delete
     * @return true if file was deleted successfully, false otherwise
     */
    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + filePath, e);
            return false;
        }
    }

    /**
     * Get file size in human-readable format
     * @param filePath Path to the file
     * @return File size as a string (e.g., "1.2 MB")
     */
    public static String getFileSize(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return "0 B";

            long size = file.length();
            if (size <= 0) return "0 B";

            final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
            return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size: " + filePath, e);
            return "Unknown";
        }
    }
}
