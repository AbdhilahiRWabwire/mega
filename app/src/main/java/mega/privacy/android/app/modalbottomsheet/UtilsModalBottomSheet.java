package mega.privacy.android.app.modalbottomsheet;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.FileProvider;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;

public class UtilsModalBottomSheet {

    public static int getPeekHeight (LinearLayout items_layout, int heightDisplay, Context context, int heightHeader) {
        int numOptions = items_layout.getChildCount();
        int numOptionsVisibles = 0;
        int heightScreen = (heightDisplay / 2);
        int heightChild = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        int peekHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightHeader, context.getResources().getDisplayMetrics());

        for (int i=0; i<numOptions; i++){
            if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE) {
                numOptionsVisibles++;
            }
        }

        if ((numOptionsVisibles <= 3 && heightHeader == 81) || (numOptionsVisibles <= 4 && heightHeader == 48)){
            peekHeight += (heightChild * numOptions);
        }
        else {
            for (int i = 0; i < numOptions; i++) {
                if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE && peekHeight < heightScreen) {
                    logDebug("Child i: " + i + " is visible; peekHeight: " + peekHeight + " heightScreen: " + heightScreen + " heightChild: " + heightChild);
                    peekHeight += heightChild;
                    if (peekHeight >= heightScreen) {
                        if (items_layout.getChildAt(i + 2) != null) {
                            boolean visible = false;
                            for (int j = i + 2; j < numOptions; j++) {
                                if (items_layout.getChildAt(j).getVisibility() == View.VISIBLE) {
                                    visible = true;
                                    break;
                                }
                            }
                            if (visible) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else if (items_layout.getChildAt(i + 1) != null) {
                            if (items_layout.getChildAt(i + 1).getVisibility() == View.VISIBLE) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else {
                            peekHeight += heightChild;
                            break;
                        }
                    }
                }
            }
        }
        return peekHeight;
    }

    public static void openWith (MegaApiAndroid megaApi, Context context, MegaNode node) {
        logDebug("openWith");

        boolean isError = false;

        String mimeType = MimeTypeList.typeForName(node.getName()).getType();
        logDebug("FILENAME: " + node.getName());

        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
        mediaIntent.putExtra("HANDLE", node.getHandle());
        mediaIntent.putExtra("FILENAME", node.getName());

        String localPath = getLocalFile(context, node.getName(), node.getSize());

        if (localPath != null) {
            File mediaFile = new File(localPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            }
            else{
                mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            }
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else {
            if (megaApi.httpServerIsRunning() == 0) {
                megaApi.httpServerStart();
            }

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);

            if(mi.totalMem> BUFFER_COMP){
                logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
            }
            else{
                logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
            }

            String url = megaApi.httpServerGetLocalLink(node);

            if(url==null){
                isError=true;
            }
            else{
                mediaIntent.setDataAndType(Uri.parse(url), mimeType);
            }
        }

        if(isError){
            Toast.makeText(context, context.getResources().getString(R.string.error_open_file_with), Toast.LENGTH_LONG).show();
        }
        else{
            if (isIntentAvailable(context, mediaIntent)){
                context.startActivity(mediaIntent);
            }
            else{
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static boolean isBottomSheetDialogShown(BottomSheetDialogFragment bottomSheetDialogFragment) {
        if (bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded()) {
            return true;
        }

        return false;
    }
}
