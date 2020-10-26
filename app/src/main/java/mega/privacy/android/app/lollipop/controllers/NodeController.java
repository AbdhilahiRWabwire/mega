package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.RemoveListener;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CopyAndSendToChatListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.download.DownloadInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.listeners.ShareListener.*;
import static mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DownloadUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class NodeController {

    Context context;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    boolean isFolderLink = false;

    public NodeController(Context context){
        logDebug("NodeController created");
        this.context = context;
        if (megaApi == null){
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public NodeController(Context context, boolean isFolderLink){
        logDebug("NodeController created");
        this.context = context;
        this.isFolderLink = isFolderLink;
        if (megaApi == null){
            if (isFolderLink) {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
            }
            else {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
            }
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void chooseLocationToCopyNodes(ArrayList<Long> handleList){
        logDebug("chooseLocationToCopyNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void copyNodes(long[] copyHandles, long toHandle) {
        logDebug("copyNodes");

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null) {
            MultipleRequestListener copyMultipleListener = null;
            if (copyHandles.length > 1) {
                logDebug("Copy multiple files");
                copyMultipleListener = new MultipleRequestListener(MULTIPLE_COPY, context);
                for (int i = 0; i < copyHandles.length; i++) {
                    MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                    if (cN != null){
                        logDebug("cN != null, i = " + i + " of " + copyHandles.length);
                        megaApi.copyNode(cN, parent, copyMultipleListener);
                    }
                    else{
                        logWarning("cN == null, i = " + i + " of " + copyHandles.length);
                    }
                }
            } else {
                logDebug("Copy one file");
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[0]);
                if (cN != null){
                    logDebug("cN != null");
                    megaApi.copyNode(cN, parent, (ManagerActivityLollipop) context);
                }
                else{
                    logWarning("cN == null");
                    if(context instanceof ManagerActivityLollipop){
                        ((ManagerActivityLollipop)context).copyError();
                    }
                }
            }
        }

    }

    public void chooseLocationToMoveNodes(ArrayList<Long> handleList){
        logDebug("chooseLocationToMoveNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
    }

    public void moveNodes(long[] moveHandles, long toHandle){
        logDebug("moveNodes");

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null){
            MultipleRequestListener moveMultipleListener = new MultipleRequestListener(MULTIPLE_MOVE, context);

            if(moveHandles.length>1){
                logDebug("MOVE multiple: " + moveHandles.length);

                for(int i=0; i<moveHandles.length;i++){
                    megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, moveMultipleListener);
                }
            }
            else{
                logDebug("MOVE single");

                megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[0]), parent, (ManagerActivityLollipop) context);
            }
        }
    }

    public void checkIfNodeIsMineAndSelectChatsToSendNode(MegaNode node) {
        logDebug("checkIfNodeIsMineAndSelectChatsToSendNode");
        ArrayList<MegaNode> nodes = new ArrayList<>();
        nodes.add(node);
        checkIfNodesAreMineAndSelectChatsToSendNodes(nodes);
    }

    public void checkIfHandlesAreMineAndSelectChatsToSendNodes(ArrayList<Long> handles) {
        ArrayList<MegaNode> nodes = new ArrayList<>();
        for (long handle : handles) {
            nodes.add(megaApi.getNodeByHandle(handle));
        }

        checkIfNodesAreMineAndSelectChatsToSendNodes(nodes);
    }

    public void checkIfNodesAreMineAndSelectChatsToSendNodes(ArrayList<MegaNode> nodes) {
        logDebug("checkIfNodesAreMineAndSelectChatsToSendNodes");

        ArrayList<MegaNode> ownerNodes = new ArrayList<>();
        ArrayList<MegaNode> notOwnerNodes = new ArrayList<>();

        if (nodes == null) {
            return;
        }

        checkIfNodesAreMine(nodes, ownerNodes, notOwnerNodes);

        if (notOwnerNodes.size() == 0) {
            selectChatsToSendNodes(ownerNodes);
            return;
        }

        CopyAndSendToChatListener copyAndSendToChatListener = new CopyAndSendToChatListener(context);
        copyAndSendToChatListener.copyNodes(notOwnerNodes, ownerNodes);
    }

    public void checkIfNodesAreMine(ArrayList<MegaNode> nodes, ArrayList<MegaNode> ownerNodes, ArrayList<MegaNode> notOwnerNodes) {
        MegaNode currentNode;

        for (int i=0; i<nodes.size(); i++) {
            currentNode = nodes.get(i);
            if (currentNode == null) continue;

            MegaNode nodeOwner = checkIfNodeIsMine(currentNode);

            if (nodeOwner != null) {
                ownerNodes.add(nodeOwner);
            }
            else {
                notOwnerNodes.add(currentNode);
            }
        }
    }

    public MegaNode checkIfNodeIsMine(MegaNode node) {
        long myUserHandle = megaApi.getMyUserHandleBinary();

        if (node.getOwner() == myUserHandle) {
            return node;
        }

        String nodeFP = megaApi.getFingerprint(node);
        ArrayList<MegaNode> fNodes = megaApi.getNodesByFingerprint(nodeFP);

        if (fNodes == null) return null;

        for (MegaNode n : fNodes) {
            if (n.getOwner() == myUserHandle) {
                return n;
            }
        }

        return null;
    }

    public void selectChatsToSendNodes(ArrayList<MegaNode> nodes){
        logDebug("selectChatsToSendNodes");

        int size = nodes.size();
        long[] longArray = new long[size];

        for(int i=0;i<nodes.size();i++){
            longArray[i] = nodes.get(i).getHandle();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(NODE_HANDLES, longArray);

        if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof FileInfoActivityLollipop) {
            ((FileInfoActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public boolean nodeComesFromIncoming (MegaNode node) {
        MegaNode parent = getParent(node);

        if (parent.getHandle() == megaApi.getRootNode().getHandle() ||
                parent.getHandle() == megaApi.getRubbishNode().getHandle() ||
                parent.getHandle() == megaApi.getInboxNode().getHandle()){
            return false;
        }
        else {
            return true;
        }
    }

    public MegaNode getParent (MegaNode node) {
        MegaNode parent = node;

        while (megaApi.getParentNode(parent) != null){
            parent = megaApi.getParentNode(parent);
        }

        return parent;
    }

    public int getIncomingLevel(MegaNode node) {
        int dBT = 0;
        MegaNode parent = node;

        while (megaApi.getParentNode(parent) != null){
            dBT++;
            parent = megaApi.getParentNode(parent);
        }

        return dBT;
    }

    public void prepareForDownload(ArrayList<Long> handleList, boolean highPriority) {
        //check permission first.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                askForPermissions();
                return;
            }
        }

        logDebug("prepareForDownload: " + handleList.size() + " files to download");
        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i = 0; i < handleList.size(); i++) {
            hashes[i] = handleList.get(i);
            MegaNode nodeTemp = megaApi.getNodeByHandle(hashes[i]);

            if (nodeTemp != null) {
                if (nodeTemp.isFile()) {
                    size += nodeTemp.getSize();
                }
            } else {
                logWarning("Error - nodeTemp is NULL");
            }

        }
        logDebug("Number of files: " + hashes.length);

        if (dbH == null) {
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean askMe = askMe(context);
        String downloadLocationDefaultPath = getDownloadLocation();

        if (askMe) {
            logDebug("askMe");
            final DownloadInfo downloadInfo = new DownloadInfo(highPriority, size, hashes);
            requestLocalFolder(downloadInfo, null);
        } else {
            logDebug("NOT askMe");
            checkSizeBeforeDownload(downloadLocationDefaultPath, null, size, hashes, highPriority);
        }
    }

    public void requestLocalFolder (DownloadInfo downloadInfo, String prompt) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select));
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, downloadInfo.getSize());
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, downloadInfo.getHashes());

        if(prompt != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_PROMPT, prompt);
        }
        intent.putExtra(HIGH_PRIORITY_TRANSFER, downloadInfo.isHighPriority());

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof FileInfoActivityLollipop){
            ((FileInfoActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof ContactFileListActivityLollipop){
            ((ContactFileListActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ((ContactInfoActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }


    //Old downloadTo
    public void checkSizeBeforeDownload(String parentPath, String url, long size, long [] hashes, boolean highPriority){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        logDebug("Files to download: " + hashes.length);
        logDebug("SIZE to download before calculating: " + size);

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        long sizeTemp=0;

        for (long hash : hashes) {
            MegaNode node = megaApi.getNodeByHandle(hash);
            if(node!=null){
                if(node.isFolder()){
                    logDebug("Node to download is FOLDER");
                    sizeTemp=sizeTemp+ getFolderSize(node, context);
                }
                else{
                    sizeTemp = sizeTemp+node.getSize();
                }
            }
        }

        final long sizeC = sizeTemp;
        logDebug("The final size is: " + getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        logDebug("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);

        if(availableFreeSpace < sizeC) {
            showNotEnoughSpaceSnackbar(context);
            logWarning("Not enough space");
            return;
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String ask=dbH.getAttributes().getAskSizeDownload();

        if(ask==null){
            ask="true";
        }

        if(ask.equals("false")){
            logDebug("SIZE: Do not ask before downloading");
            checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
        }
        else{
            logDebug("SIZE: Ask before downloading");
            //Check size to download
            //100MB=104857600
            //10MB=10485760
            //1MB=1048576
            if (sizeC > 104857600) {
                logDebug("Show size confirmacion: " + sizeC);
                //Show alert
                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC,urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof FullScreenImageViewerLollipop) {
                    ((FullScreenImageViewerLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof FileInfoActivityLollipop) {
                    ((FileInfoActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof ContactFileListActivityLollipop) {
                    ((ContactFileListActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof PdfViewerActivityLollipop) {
                    ((PdfViewerActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof AudioVideoPlayerLollipop) {
                    ((AudioVideoPlayerLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof ContactInfoActivityLollipop) {
                    ((ContactInfoActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
            } else {
                checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
            }
        }
    }

    //Old proceedToDownload
    public void checkInstalledAppBeforeDownload(String parentPath, String url, long size, long [] hashes, boolean highPriority){
        logDebug("checkInstalledAppBeforeDownload");
        boolean confirmationToDownload = false;
        final String parentPathC = parentPath;
        final String urlC = url;
        final long sizeC = size;
        final long [] hashesC = hashes;
        String nodeToDownload = null;

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String ask=dbH.getAttributes().getAskNoAppDownload();

        if(ask==null){
            logDebug("ask==null");
            ask="true";
        }

        if(ask.equals("false")){
            logDebug("INSTALLED APP: Do not ask before downloading");
            download(parentPathC, urlC, sizeC, hashesC, highPriority);
        }
        else{
            logDebug("INSTALLED APP: Ask before downloading");
            if (hashes != null){
                for (long hash : hashes) {
                    MegaNode node = megaApi.getNodeByHandle(hash);
                    if(node!=null){
                        logDebug("Node: " + node.getHandle());

                        if(node.isFile()){
                            Intent checkIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            logDebug("MimeTypeList: " + MimeTypeList.typeForName(node.getName()).getType());

                            checkIntent.setType(MimeTypeList.typeForName(node.getName()).getType());

                            try{
                                if (!isIntentAvailable(context, checkIntent)){
                                    confirmationToDownload = true;
                                    nodeToDownload=node.getName();
                                    break;
                                }
                            }catch(Exception e){
                                logWarning("isIntent EXCEPTION", e);
                                confirmationToDownload = true;
                                nodeToDownload=node.getName();
                                break;
                            }
                        }
                    }
                    else{
                        logWarning("ERROR - node is NULL");
                    }
                }
            }

            //Check if show the alert message
            if(confirmationToDownload){
                //Show message
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof FullScreenImageViewerLollipop){
                    ((FullScreenImageViewerLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof PdfViewerActivityLollipop){
                    ((PdfViewerActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof AudioVideoPlayerLollipop){
                    ((AudioVideoPlayerLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof ContactInfoActivityLollipop){
                    ((ContactInfoActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC,urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
            }
            else{
                download(parentPathC, urlC, sizeC, hashesC, highPriority);
            }
        }
    }

    private void askForPermissions () {
        if(context instanceof ManagerActivityLollipop){
            ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if (context instanceof FileLinkActivityLollipop) {
            ActivityCompat.requestPermissions((FileLinkActivityLollipop)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof FullScreenImageViewerLollipop){
            ActivityCompat.requestPermissions(((FullScreenImageViewerLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof FileInfoActivityLollipop){
            ActivityCompat.requestPermissions(((FileInfoActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof ContactFileListActivityLollipop){
            ActivityCompat.requestPermissions(((ContactFileListActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ActivityCompat.requestPermissions(((ContactInfoActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    public void download(String parentPath, String url, long size, long[] hashes, boolean highPriority) {
        logDebug("files to download: " + hashes.length);
        if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        SDCardOperator sdCardOperator = SDCardOperator.initSDCardOperator(context, parentPath);
        if (sdCardOperator == null) {
            requestLocalFolder(new DownloadInfo(highPriority, size, hashes), context.getString(R.string.no_external_SD_card_detected));
            return;
        }

        MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);

        if (hashes.length == 1 && tempNode != null && tempNode.getType() == MegaNode.TYPE_FILE) {
            String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize());
            //Check if the file is already downloaded, and downloaded file is the latest version
            if (localPath != null
                    && isFileDownloadedLatest(new File(localPath), tempNode)) {
                checkDownload(context, tempNode, localPath, parentPath, true, sdCardOperator);

                if (!Boolean.parseBoolean(dbH.getAutoPlayEnabled())) {
                    return;
                }

                if (MimeTypeList.typeForName(tempNode.getName()).isZip()) {
                    File zipFile = new File(localPath);

                    Intent intentZip = new Intent();
                    intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                    intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                    intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                    context.startActivity(intentZip);

                } else if (MimeTypeList.typeForName(tempNode.getName()).isPdf()) {
                    if (context instanceof PdfViewerActivityLollipop) {
                        ((PdfViewerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                    } else {
                        File pdfFile = new File(localPath);

                        Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
                        pdfIntent.putExtra("HANDLE", tempNode.getHandle());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            pdfIntent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        } else {
                            pdfIntent.setDataAndType(Uri.fromFile(pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        pdfIntent.putExtra("inside", true);
                        pdfIntent.putExtra("isUrl", false);
                        context.startActivity(pdfIntent);
                    }
                } else if (MimeTypeList.typeForName(tempNode.getName()).isVideoReproducible() || MimeTypeList.typeForName(tempNode.getName()).isAudio()) {
                    logDebug("Video/Audio file");
                    if (context instanceof AudioVideoPlayerLollipop) {
                        ((AudioVideoPlayerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                    } else {
                        File mediaFile = new File(localPath);

                        Intent mediaIntent;
                        boolean internalIntent;
                        boolean opusFile = false;
                        if (MimeTypeList.typeForName(mediaFile.getName()).isVideoNotSupported() || MimeTypeList.typeForName(mediaFile.getName()).isAudioNotSupported()) {
                            mediaIntent = new Intent(Intent.ACTION_VIEW);
                            internalIntent = false;
                            String[] s = mediaFile.getName().split("\\.");
                            if (s.length > 1 && s[s.length - 1].equals("opus")) {
                                opusFile = true;
                            }
                        } else {
                            internalIntent = true;
                            mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                        }
                        mediaIntent.putExtra(IS_PLAYLIST, false);
                        mediaIntent.putExtra("HANDLE", tempNode.getHandle());
                        mediaIntent.putExtra(AudioVideoPlayerLollipop.PLAY_WHEN_READY, MegaApplication.getInstance().isActivityVisible());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());

                        } else {
                            mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        }
                        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mediaIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        if (opusFile) {
                            mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                        }
                        if (internalIntent) {
                            context.startActivity(mediaIntent);
                        } else {
                            if (isIntentAvailable(context, mediaIntent)) {
                                context.startActivity(mediaIntent);
                            } else {
                                showSnackbar(context, context.getString(R.string.intent_not_available));
                                Intent intentShare = new Intent(Intent.ACTION_SEND);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    intentShare.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    intentShare.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (isIntentAvailable(context, intentShare)) {
                                    context.startActivity(intentShare);
                                }
                            }
                        }
                    }
                } else {
                    if (context instanceof FullScreenImageViewerLollipop) {
                        ((FullScreenImageViewerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                    } else {
                        try {
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                viewIntent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            } else {
                                viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            if (isIntentAvailable(context, viewIntent)) {
                                context.startActivity(viewIntent);
                            } else {
                                Intent intentShare = new Intent(Intent.ACTION_SEND);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    intentShare.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (isIntentAvailable(context, intentShare)) {
                                    context.startActivity(intentShare);
                                }
                                showSnackbar(context, context.getString(R.string.general_already_downloaded));
                            }
                        } catch (Exception e) {
                            showSnackbar(context, context.getString(R.string.general_already_downloaded));
                        }
                    }
                }
                return;
            } else {
                logWarning("localPath is NULL");
            }
        }

        int numberOfNodesAlreadyDownloaded = 0;
        int numberOfNodesPending = 0;
        int emptyFolders = 0;

        for (long hash : hashes) {
            MegaNode node = megaApi.getNodeByHandle(hash);
            if (node != null) {
                logDebug("node NOT null");
                Map<MegaNode, String> dlFiles = new HashMap<>();
                Map<Long, String> targets = new HashMap<>();
                if (node.getType() == MegaNode.TYPE_FOLDER) {
                    if (sdCardOperator.isSDCardDownload()) {
                        sdCardOperator.buildFileStructure(targets, parentPath, megaApi, node);
                        getDlList(dlFiles, node, new File(sdCardOperator.getDownloadRoot(), node.getName()));
                    } else {
                        getDlList(dlFiles, node, new File(parentPath, node.getName()));
                    }
                } else {
                    if (sdCardOperator.isSDCardDownload()) {
                        targets.put(node.getHandle(), parentPath);
                        dlFiles.put(node, sdCardOperator.getDownloadRoot());
                    } else {
                        dlFiles.put(node, parentPath);
                    }
                }

                if (dlFiles.isEmpty()) {
                    emptyFolders++;
                }

                for (MegaNode document : dlFiles.keySet()) {
                    String path = dlFiles.get(document);
                    String targetPath = targets.get(document.getHandle());

                    if (isTextEmpty(path)) {
                        continue;
                    }

                    File destDir = new File(path);
                    File destFile;
                    if (destDir.isDirectory()) {
                        destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName(), destDir.getAbsolutePath() + SEPARATOR));
                    } else {
                        destFile = destDir;
                    }

                    if (isFileAvailable(destFile)
                            && document.getSize() == destFile.length()
                            && isFileDownloadedLatest(destFile, document)) {
                        numberOfNodesAlreadyDownloaded++;
                    } else {
                        numberOfNodesPending++;
                        Intent service = new Intent(context, DownloadService.class);
                        service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                        service.putExtra(DownloadService.EXTRA_URL, url);
                        if (sdCardOperator.isSDCardDownload()) {
                            service = getDownloadToSDCardIntent(service, path, targetPath, dbH.getSDCardUri());
                        } else {
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                        }
                        service.putExtra(DownloadService.EXTRA_URL, url);
                        service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                        service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                        if (highPriority) {
                            service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                        }
                        if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof FullScreenImageViewerLollipop) {
                            service.putExtra("fromMV", true);
                        }
                        context.startService(service);
                    }
                }
            } else if (url != null) {
                Intent service = new Intent(context, DownloadService.class);
                service.putExtra(DownloadService.EXTRA_HASH, hash);
                service.putExtra(DownloadService.EXTRA_URL, url);
                service.putExtra(DownloadService.EXTRA_SIZE, size);
                service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                if (highPriority) {
                    service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                }
                if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof FullScreenImageViewerLollipop) {
                    service.putExtra("fromMV", true);
                }
                context.startService(service);
            } else {
                logWarning("Node NOT fOUND!!!!!");
            }

            showSnackBarWhenDownloading(context, numberOfNodesPending, numberOfNodesAlreadyDownloaded, emptyFolders);
        }
    }

    /*
	 * Get list of all child files
	 */
    private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
        logDebug("getDlList");
        if (megaApi.getRootNode() == null)
            return;

        folder.mkdir();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for(int i=0; i<nodeList.size(); i++){
            MegaNode document = nodeList.get(i);
            if (document.getType() == MegaNode.TYPE_FOLDER) {
                File subfolder = new File(folder, new String(document.getName()));
                getDlList(dlFiles, document, subfolder);
            }
            else {
                dlFiles.put(document, folder.getAbsolutePath());
            }
        }
    }

    public void renameNode(MegaNode document, String newName){
        logDebug("renameNode");
        if (newName.compareTo(document.getName()) == 0) {
            return;
        }

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        logDebug("Renaming " + document.getName() + " to " + newName);

        megaApi.renameNode(document, newName, ((ManagerActivityLollipop) context));
    }

    public int importLink(String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        }
        catch (Exception e) {
            logError("Error decoding URL: " + url, e);
        }

        url.replace(' ', '+');
        if(url.startsWith("mega://")){
            url = url.replace("mega://", "https://mega.co.nz/");
        }

        logDebug("url " + url);

        // Download link
        if (AndroidMegaRichLinkMessage.isFileLink(url)) {
            Intent openFileIntent = new Intent(context, FileLinkActivityLollipop.class);
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            ((ManagerActivityLollipop) context).startActivity(openFileIntent);
            return FILE_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isFolderLink(url)) {
            Intent openFolderIntent = new Intent(context, FolderLinkActivityLollipop.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            context.startActivity(openFolderIntent);
            return FOLDER_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isChatLink(url)) {
            return CHAT_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isContactLink(url)) {
            return CONTACT_LINK;
        }

        logWarning("wrong url");
        return ERROR_LINK;
    }

    //old getPublicLinkAndShareIt
    public void exportLink(MegaNode document){
        logDebug("exportLink");
        if (!isOnline(context)) {
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
            return;
        }
        else if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((ManagerActivityLollipop) context));
        }
        else if(context instanceof GetLinkActivityLollipop){
            megaApi.exportNode(document, ((GetLinkActivityLollipop) context));
        }
        else  if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FullScreenImageViewerLollipop) context));
        }
        else  if(context instanceof FileInfoActivityLollipop){
            ((FileInfoActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FileInfoActivityLollipop) context));
        }
    }

    public void exportLinkTimestamp(MegaNode document, int timestamp){
        logDebug("exportLinkTimestamp: " + timestamp);
        if (!isOnline(context)) {
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
        }
        else if (context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((ManagerActivityLollipop) context));
        }
        else if (context instanceof GetLinkActivityLollipop){
            megaApi.exportNode(document, timestamp, ((GetLinkActivityLollipop) context));
        }
        else if (context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((FullScreenImageViewerLollipop) context));
        }
        else if (context instanceof FileInfoActivityLollipop){
            megaApi.exportNode(document, timestamp, ((FileInfoActivityLollipop) context));
        }
    }

    public void removeLink(MegaNode document, ExportListener exportListener){
        megaApi.disableExport(document, exportListener);
    }

    public void removeLinks(ArrayList<MegaNode> nodes){
        if (!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        ExportListener exportListener = new ExportListener(context, true, nodes.size());

        for (MegaNode node : nodes) {
            removeLink(node, exportListener);
        }
    }


    public void selectContactToShareFolders(ArrayList<Long> handleList){
        logDebug("shareFolders ArrayListLong");
        //TODO shareMultipleFolders

        if (!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivityLollipop.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);

        long[] handles=new long[handleList.size()];
        int j=0;
        for(int i=0; i<handleList.size();i++){
            handles[j]=handleList.get(i);
            j++;
        }
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, handles);
        //Multiselect=1 (multiple folders)
        intent.putExtra("MULTISELECT", 1);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void selectContactToShareFolder(MegaNode node){
        logDebug("shareFolder");

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivityLollipop.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);
        //Multiselect=0
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void moveToTrash(final ArrayList<Long> handleList, boolean moveToRubbish){
        logDebug("moveToTrash: " + moveToRubbish);

        MultipleRequestListener moveMultipleListener = null;
        MegaNode parent;
        //Check if the node is not yet in the rubbish bin (if so, remove it)
        if(handleList!=null){
            if(handleList.size()>1){
                logDebug("MOVE multiple: " + handleList.size());
                if (moveToRubbish){
                    moveMultipleListener = new MultipleRequestListener(MULTIPLE_SEND_RUBBISH, context);
                }
                else{
                    moveMultipleListener = new MultipleRequestListener(MULTIPLE_MOVE, context);
                }
                for (int i=0;i<handleList.size();i++){
                    if (moveToRubbish){
                        megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), megaApi.getRubbishNode(), moveMultipleListener);

                    }
                    else{
                        megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), moveMultipleListener);
                    }
                }
            }
            else{
                logDebug("MOVE single");
                if (moveToRubbish){
                    megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), ((ManagerActivityLollipop) context));
                }
                else{
                    megaApi.remove(megaApi.getNodeByHandle(handleList.get(0)), ((ManagerActivityLollipop) context));
                }
            }
        }
        else{
            logWarning("handleList NULL");
            return;
        }
    }

    public void openFolderFromSearch(long folderHandle){
        logDebug("openFolderFromSearch: " + folderHandle);
        ((ManagerActivityLollipop)context).textSubmitted = true;
        ((ManagerActivityLollipop)context).openFolderRefresh = true;
        boolean firstNavigationLevel=true;
        int access = -1;
        ManagerActivityLollipop.DrawerItem drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
        if (folderHandle != -1) {
            MegaNode parentIntentN = megaApi.getParentNode(megaApi.getNodeByHandle(folderHandle));
            if (parentIntentN != null) {
                logDebug("Check the parent node: " + parentIntentN.getName() + " handle: " + parentIntentN.getHandle());
                access = megaApi.getAccess(parentIntentN);
                switch (access) {
                    case MegaShare.ACCESS_OWNER:
                    case MegaShare.ACCESS_UNKNOWN: {
                        //Not incoming folder, check if Cloud or Rubbish tab
                        if(parentIntentN.getHandle()==megaApi.getRootNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                            logDebug("Navigate to TAB CLOUD first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                        }
                        else if(parentIntentN.getHandle()==megaApi.getRubbishNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.RUBBISH_BIN;
                            logDebug("Navigate to TAB RUBBISH first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                        }
                        else if(parentIntentN.getHandle()==megaApi.getInboxNode().getHandle()){
                            logDebug("Navigate to INBOX first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                            drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                        }
                        else{
                            int parent = checkParentNodeToOpenFolder(parentIntentN.getHandle());
                            logDebug("The parent result is: " + parent);

                            switch (parent){
                                case 0:{
                                    //ROOT NODE
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    logDebug("Navigate to TAB CLOUD with parentHandle");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 1:{
                                    logDebug("Navigate to TAB RUBBISH");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.RUBBISH_BIN;
                                    ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 2:{
                                    logDebug("Navigate to INBOX WITH parentHandle");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                                    ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case -1:{
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    logDebug("Navigate to TAB CLOUD general");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(-1);
                                    firstNavigationLevel=true;
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_FULL: {
                        logDebug("GO to INCOMING TAB: " + parentIntentN.getName());
                        drawerItem = ManagerActivityLollipop.DrawerItem.SHARED_ITEMS;
                        if(parentIntentN.getHandle()==-1){
                            logDebug("Level 0 of Incoming");
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(0);
                            firstNavigationLevel=true;
                        }
                        else{
                            firstNavigationLevel=false;
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(parentIntentN.getHandle());
                            int deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, context);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(deepBrowserTreeIncoming);
                            logDebug("After calculating deepBrowserTreeIncoming: " + deepBrowserTreeIncoming);
                        }
                        ((ManagerActivityLollipop) context).setTabItemShares(0);
                        break;
                    }
                    default: {
                        logDebug("DEFAULT: The intent set the parentHandleBrowser to " + parentIntentN.getHandle());
                        ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                        drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                        firstNavigationLevel=true;
                        break;
                    }
                }
            }
            else{
                logWarning("Parent is already NULL");

                drawerItem = ManagerActivityLollipop.DrawerItem.SHARED_ITEMS;
                ((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
                ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(0);
                firstNavigationLevel=true;
                ((ManagerActivityLollipop) context).setTabItemShares(0);
            }
            ((ManagerActivityLollipop) context).setFirstNavigationLevel(firstNavigationLevel);
            ((ManagerActivityLollipop) context).setDrawerItem(drawerItem);
            ((ManagerActivityLollipop) context).selectDrawerItemLollipop(drawerItem);
        }
    }

    public int checkParentNodeToOpenFolder(long folderHandle){
        logDebug("Folder handle: " + folderHandle);
        MegaNode folderNode = megaApi.getNodeByHandle(folderHandle);
        MegaNode parentNode = megaApi.getParentNode(folderNode);
        if(parentNode!=null){
            logDebug("Parent handle: "+parentNode.getHandle());
            if(parentNode.getHandle()==megaApi.getRootNode().getHandle()){
                logDebug("The parent is the ROOT");
                return 0;
            }
            else if(parentNode.getHandle()==megaApi.getRubbishNode().getHandle()){
                logDebug("The parent is the RUBBISH");
                return 1;
            }
            else if(parentNode.getHandle()==megaApi.getInboxNode().getHandle()){
                logDebug("The parent is the INBOX");
                return 2;
            }
            else if(parentNode.getHandle()==-1){
                logWarning("The parent is -1");
                return -1;
            }
            else{
                int result = checkParentNodeToOpenFolder(parentNode.getHandle());
                logDebug("Call returns " + result);
                switch(result){
                    case -1:
                        return -1;
                    case 0:
                        return 0;
                    case 1:
                        return 1;
                    case 2:
                        return 2;
                }
            }
        }
        return -1;
    }

    public void leaveIncomingShare (final MegaNode n){
        logDebug("Node handle: " + n.getHandle());
        megaApi.remove(n, new RemoveListener(context, true));
    }

    public void leaveMultipleIncomingShares (final ArrayList<Long> handleList){
        logDebug("Leaving " + handleList.size() + " incoming shares");

        if (handleList.size() == 1) {
            leaveIncomingShare(megaApi.getNodeByHandle(handleList.get(0)));
            return;
        }

        MultipleRequestListener moveMultipleListener = new MultipleRequestListener(MULTIPLE_LEAVE_SHARE, context);
        for (int i = 0; i < handleList.size(); i++) {
            MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
            megaApi.remove(node, moveMultipleListener);
        }
    }

    public void removeShares(ArrayList<MegaShare> listShares, MegaNode node){
        if (listShares == null || listShares.isEmpty()) return;

        ShareListener shareListener = new ShareListener(context, REMOVE_SHARE_LISTENER, listShares.size());

        for (MegaShare share : listShares) {
            String email = share.getUser();
            if (email != null) {
                removeShare(shareListener, node, email);
            }
        }
    }

    public void removeSeveralFolderShares(List<MegaNode> nodes) {
        ArrayList<MegaShare> totalShares = new ArrayList<>();

        for (MegaNode node : nodes) {
            ArrayList<MegaShare> shares = megaApi.getOutShares(node);
            if (shares != null && !shares.isEmpty()) {
                totalShares.addAll(shares);
            }
        }

        ShareListener shareListener = new ShareListener(context, REMOVE_SHARE_LISTENER, totalShares.size());

        for (MegaShare megaShare : totalShares) {
            MegaNode node = megaApi.getNodeByHandle(megaShare.getNodeHandle());
            String email = megaShare.getUser();
            if (node != null && email != null) {
                removeShare(shareListener, node, email);
            }
        }
    }

    public void removeShare(ShareListener shareListener, MegaNode node, String email){
        megaApi.share(node, email, MegaShare.ACCESS_UNKNOWN, shareListener);
    }

    public void shareFolder(MegaNode node, ArrayList<String> selectedContacts, int permissions) {
        if (!isOnline(context)) {
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (selectedContacts == null || selectedContacts.isEmpty()) return;

        ShareListener shareListener = new ShareListener(context, SHARE_LISTENER, selectedContacts.size());

        for (int i = 0; i < selectedContacts.size(); i++) {
            shareFolder(node, selectedContacts.get(i), permissions, shareListener);
        }
    }

    public void shareFolders(long[] nodeHandles, ArrayList<String> contactsData, int permissions){

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (nodeHandles == null || nodeHandles.length == 0) return;

        for (int i = 0; i < nodeHandles.length; i++) {
            shareFolder(megaApi.getNodeByHandle(nodeHandles[i]), contactsData, permissions);
        }
    }

    public void shareFolder(MegaNode node, String email, int permissions, ShareListener shareListener) {
        if (node == null || email == null) return;

        megaApi.share(node, email, permissions, shareListener);
    }

    public void cleanRubbishBin(){
        logDebug("cleanRubbishBin");
        megaApi.cleanRubbishBin((ManagerActivityLollipop) context);
    }

    public void clearAllVersions(){
        logDebug("clearAllVersions");
        megaApi.removeVersions((ManagerActivityLollipop) context);
    }

    public void deleteOffline(MegaOffline selectedNode){
        logDebug("deleteOffline");
        dbH = DatabaseHandler.getDbHandler(context);

        //Delete children
        ArrayList<MegaOffline> mOffListChildren = dbH.findByParentId(selectedNode.getId());
        if (mOffListChildren.size() > 0) {
            //The node have childrens, delete
            deleteChildrenDB(mOffListChildren);
        }

        removeNodePhysically(selectedNode);

        dbH.removeById(selectedNode.getId());

        //Check if the parent has to be deleted

        int parentId = selectedNode.getParentId();
        MegaOffline parentNode = dbH.findById(parentId);

        if (parentNode != null) {
            logDebug("Parent to check: " + parentNode.getName());
            checkParentDeletion(parentNode);
        }

        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).updateOfflineView(null);
        }
    }

    private void removeNodePhysically(MegaOffline megaOffline) {
        logDebug("Remove the node physically");
        try {
            File offlineFile = getOfflineFile(context, megaOffline);
            deleteFolderAndSubfolders(context, offlineFile);
        } catch (Exception e) {
            logError("EXCEPTION: deleteOffline - adapter", e);
        }
    }

    public void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){

        logDebug("Size: " + mOffListChildren.size());
        MegaOffline mOffDelete=null;

        for(int i=0; i<mOffListChildren.size(); i++){

            mOffDelete=mOffListChildren.get(i);

            logDebug("Children " + i + ": "+ mOffDelete.getHandle());
            ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
            if(mOffListChildren2.size()>0){
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2);
            }

            int lines = dbH.removeById(mOffDelete.getId());
            logDebug("Deleted: " + lines);
        }
    }

    public void checkParentDeletion (MegaOffline parentToDelete){
        logDebug("parentToDelete: " + parentToDelete.getHandle());

        ArrayList<MegaOffline> mOffListChildren=dbH.findByParentId(parentToDelete.getId());
        File destination = null;
        if(mOffListChildren.size()<=0){
            logDebug("The parent has NO children");
            //The node have NO childrens, delete it

            dbH.removeById(parentToDelete.getId());

            removeNodePhysically(parentToDelete);

            int parentId = parentToDelete.getParentId();
            if(parentId==-1){
                File rootIncomingFile = getOfflineFile(context, parentToDelete);

                if(isFileAvailable(rootIncomingFile)){
                    String[] fileList = rootIncomingFile.list();
                    if(fileList!=null){
                        if(rootIncomingFile.list().length==0){
                            try{
                                rootIncomingFile.delete();
                            }
                            catch(Exception e){
                                logError("EXCEPTION: deleteParentIncoming: " + destination, e);
                            };
                        }
                    }
                }
                else{
                    logWarning("rootIncomingFile is NULL");
                }
            }
            else{
                //Check if the parent has to be deleted

                parentToDelete = dbH.findById(parentId);
                if(parentToDelete != null){
                    logDebug("Parent to check: " + parentToDelete.getHandle());
                    checkParentDeletion(parentToDelete);

                }
            }

        }
        else{
            logDebug("The parent has children!!! RETURN!!");
            return;
        }

    }

    public void downloadFileLink (final MegaNode document, final String url) {
        logDebug("downloadFileLink");

        if (document == null){
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                askForPermissions();
                return;
            }
        }


        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }

        if (dbH.getCredentials() == null || dbH.getPreferences() == null) {
            intentPickFolder(document, url);
            return;
        }

        boolean askMe = askMe(context);
        if (askMe) {
            intentPickFolder(document, url);
        } else {
            String downloadLocationDefaultPath = getDownloadLocation();
            downloadTo(document, downloadLocationDefaultPath, url);
        }
    }

    private void intentPickFolder(MegaNode document, String url) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.context_download_to));
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());

        if (context instanceof FileLinkActivityLollipop) {
            ((FileLinkActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof FullScreenImageViewerLollipop) {
            ((FullScreenImageViewerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }

    public void downloadTo(MegaNode currentDocument, String parentPath, String url){
        logDebug("downloadTo");

        if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        SDCardOperator sdCardOperator = SDCardOperator.initSDCardOperator(context, parentPath);
        if(sdCardOperator == null) {
            intentPickFolder(currentDocument, url);
            return;
        }

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }catch(Exception ex){}

        final MegaNode tempNode = currentDocument;
        if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
            logDebug("is file");
            final String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize());
            if(localPath != null){
                checkDownload(context, tempNode, localPath, parentPath, false, sdCardOperator);
            } else{
                logDebug("LocalPath is NULL");
                showSnackbar(context, context.getResources().getQuantityString(R.plurals.download_began, 1, 1));

                if(tempNode != null){
                    logDebug("Node!=null: "+tempNode.getName());
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    dlFiles.put(tempNode, parentPath);

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);

                        if(availableFreeSpace < document.getSize()){
                            showNotEnoughSpaceSnackbar(context);
                            continue;
                        }

                        Intent service = new Intent(context, DownloadService.class);
                        service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                        service.putExtra(EXTRA_SERIALIZE_STRING, currentDocument.serialize());
                        service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                        if (sdCardOperator.isSDCardDownload()) {
                            service = getDownloadToSDCardIntent(service, sdCardOperator.getDownloadRoot(), path, dbH.getSDCardUri());
                        } else {
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                        }
                        logDebug("intent to DownloadService");
                        if (context instanceof AudioVideoPlayerLollipop || context instanceof FullScreenImageViewerLollipop || context instanceof PdfViewerActivityLollipop) {
                            service.putExtra("fromMV", true);
                        }
                        context.startService(service);
                    }
                }
                else if(url != null) {
                    if(availableFreeSpace < currentDocument.getSize()) {
                        showNotEnoughSpaceSnackbar(context);
                    }

                    Intent service = new Intent(context, DownloadService.class);
                    service.putExtra(DownloadService.EXTRA_HASH, currentDocument.getHandle());
                    service.putExtra(EXTRA_SERIALIZE_STRING, currentDocument.serialize());
                    service.putExtra(DownloadService.EXTRA_SIZE, currentDocument.getSize());
                    service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                    if (context instanceof AudioVideoPlayerLollipop || context instanceof FullScreenImageViewerLollipop || context instanceof PdfViewerActivityLollipop) {
                        service.putExtra("fromMV", true);
                    }
                    context.startService(service);
                }
                else {
                    logWarning("Node not found. Let's try the document");
                }
            }
        }
    }

    public static Intent getDownloadToSDCardIntent(Intent intent,String downloadRoot,String targetPath,String uri) {
        intent.putExtra(DownloadService.EXTRA_PATH, downloadRoot);
        intent.putExtra(DownloadService.EXTRA_DOWNLOAD_TO_SDCARD, true);
        intent.putExtra(DownloadService.EXTRA_TARGET_PATH, targetPath);
        intent.putExtra(DownloadService.EXTRA_TARGET_URI, uri);
        return intent;
    }
}
