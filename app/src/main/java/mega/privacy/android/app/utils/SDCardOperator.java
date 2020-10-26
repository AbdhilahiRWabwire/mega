package mega.privacy.android.app.utils;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;

public class SDCardOperator {

    public final static String TEST = "test";
    public final static String TEMP = "temp";
    public final static String SD_PATH_REGEX = "^/storage/\\w{4}-\\w{4}/?.*$";

    private Context context;

    private String downloadRoot;

    private String sdCardRoot;

    private String targetRoot;

    private DocumentFile sdCardRootDocument;

    private boolean isSDCardDownload;

    //32kb
    private static final int BUFFER_SIZE = 32 * 1024;

    public static class SDCardException extends Exception {

        public SDCardException(String message) {
            super(message);
        }

        private SDCardException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private SDCardOperator(boolean isSDCardDownload) {
        this.isSDCardDownload = isSDCardDownload;
    }

    public SDCardOperator(Context context) throws SDCardException {
        this.context = context;
        //package cache folder on sd card.
        File[] fs = context.getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            downloadRoot = fs[1].getAbsolutePath() + File.separator + System.currentTimeMillis();
        } else {
            throw new SDCardException("No sd card installed!");
        }
        sdCardRoot = SDCardUtils.getSDCardRoot(downloadRoot);
    }

    public void initDocumentFileRoot(String uriString) throws SDCardException {
        if (TextUtils.isEmpty(uriString)) {
            throw new SDCardException("Haven't got sd card root uri!");
        } else {
            try {
                Uri tragetUri = Uri.parse(uriString);
                targetRoot = getFullPathFromTreeUri(tragetUri, context);
                sdCardRootDocument = DocumentFile.fromTreeUri(context, tragetUri);
                if (sdCardRootDocument == null || !sdCardRootDocument.canWrite()) {
                    throw new SDCardException("Permission required!");
                }
            } catch (Exception e) {
                if (!(e instanceof SDCardException)) {
                    throw new SDCardException("Invalid uri string.", e);
                } else {
                    throw e;
                }
            }
        }
    }

    public String getDownloadRoot() {
        return downloadRoot;
    }

    public String getSDCardRoot() {
        return sdCardRoot;
    }

    private boolean isNewSDCardPath(String path) {
        return !path.startsWith(sdCardRoot);
    }

    public static boolean isSDCardPath(String path) {
        return path.matches(SD_PATH_REGEX);
    }

    public boolean canWriteWithFile(String target) {
        boolean canWrite = new File(target).canWrite();
        if (!canWrite) {
            return false;
        }
        // test if really can write on this path.
        File test = new File(target, TEST);
        try {
            canWrite = test.createNewFile();
        } catch (IOException e) {
            return false;
        }
        if (!canWrite) {
            return false;
        } else {
            return test.delete();
        }
    }

    public String move(String targetPath, File source) throws IOException {
        moveFile(targetPath, source);
        return targetPath + File.separator + source.getName();
    }

    public void buildFileStructure(Map<Long, String> dlList, String parent, MegaApiJava api, MegaNode node) {
        if (node.isFolder()) {
            createFolder(parent, node.getName());
            parent += (File.separator + node.getName());
            ArrayList<MegaNode> children = api.getChildren(node);
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    MegaNode child = children.get(i);
                    if (child.isFolder()) {
                        buildFileStructure(dlList, parent, api, child);
                    } else {
                        dlList.put(child.getHandle(), parent);
                    }
                }
            }
        }
    }

    private DocumentFile getDocumentFileByPath(String parentPath) {
        DocumentFile root = sdCardRootDocument;
        for (String folder : getSubFolders(targetRoot, parentPath)) {
            if (root.findFile(folder) == null) {
                root = root.createDirectory(folder);
            } else {
                root = root.findFile(folder);
            }
        }
        return root;
    }

    public DocumentFile createFolder(String path, String name) {
        DocumentFile parent = getDocumentFileByPath(path);
        DocumentFile folder = parent.findFile(name);
        if (folder != null) {
            return folder;
        }
        return parent.createDirectory(name);
    }

    void moveFile(String targetPath, File file) throws IOException {
        String name = file.getName();
        DocumentFile parent = getDocumentFileByPath(targetPath);
        DocumentFile df = parent.findFile(name);
        //alreay exists
        if (df != null && df.length() == file.length()) {
            logDebug(name + " already exists.");
            return;
        }
        //update
        if (df != null && df.length() != file.length()) {
            logDebug("delete former file.");
            df.delete();
        }
        Uri uri = parent.createFile(null, name).getUri();
        OutputStream os = null;
        InputStream is = null;
        try {
            os = context.getContentResolver().openOutputStream(uri, "rwt");
            if (os == null) {
                throw new IOException("open output stream exception!");
            }
            is = new FileInputStream(file);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
                os.flush();
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    private List<String> getSubFolders(String root, String parent) {
        if (parent.length() < root.length()) {
            throw new IllegalArgumentException("no subfolders!");
        }
        if (!parent.contains(root)) {
            throw new IllegalArgumentException("parent is not a subfolder of root!");
        }
        List<String> folders = new ArrayList<>();
        for (String s : parent.substring(root.length()).split(File.separator)) {
            if (!"".equals(s)) {
                folders.add(s);
            }
        }
        return folders;
    }

    public boolean isSDCardDownload() {
        return isSDCardDownload;
    }

    private void setSDCardDownload(boolean SDCardDownload) {
        isSDCardDownload = SDCardDownload;
    }

    /**
     * Checks if the download path belongs to an SD card.
     *
     * @param context       current Context
     * @param downloadPath  download path
     * @return The new SDCardOperator to continue its initialization.
     */
    private static SDCardOperator checkDownloadPath(Context context, String downloadPath) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        boolean isSDCardPath = isSDCardPath(downloadPath);
        SDCardOperator sdCardOperator = null;

        try {
            sdCardOperator = new SDCardOperator(context);
        } catch (SDCardOperator.SDCardException e) {
            e.printStackTrace();
            logError("Initialize SDCardOperator failed", e);
            // user uninstall the sd card. but default download location is still on the sd card
            if (isSDCardPath) {
                logDebug("select new path as download location.");
                new Handler().postDelayed(() -> Toast.makeText(MegaApplication.getInstance(), R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show(), 1000);
                return null;
            }
        }
        if (sdCardOperator != null && isSDCardPath) {
            //user has installed another sd card.
            if (sdCardOperator.isNewSDCardPath(downloadPath)) {
                logDebug("new sd card, check permission.");
                new Handler().postDelayed(() -> Toast.makeText(MegaApplication.getInstance(), R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show(), 1000);
                return null;
            }

            if (!sdCardOperator.canWriteWithFile(downloadPath)) {
                try {
                    sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("SDCardOperator initDocumentFileRoot failed requestSDCardPermission", e);
                    return null;
                }
            }
        }
        return sdCardOperator;
    }

    /**
     * Inits the SDCardOperator.
     *
     * @param context       current Context
     * @param parentPath    SD card parent path
     * @return The initialized SDCardOperator.
     */
    public static SDCardOperator initSDCardOperator(Context context, String parentPath) {
        SDCardOperator sdCardOperator;

        if(SDCardOperator.isSDCardPath(parentPath)) {
            sdCardOperator =  checkDownloadPath(context, parentPath);
            if (sdCardOperator != null) {
                sdCardOperator.setSDCardDownload(!isTextEmpty(sdCardOperator.getDownloadRoot()));
            }
        } else {
            sdCardOperator = new SDCardOperator(false);
        }

        return sdCardOperator;
    }
}
