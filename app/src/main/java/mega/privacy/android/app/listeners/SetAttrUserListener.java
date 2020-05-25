package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaStringMap;

import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.FragmentTag.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.*;

public class SetAttrUserListener extends BaseListener {

    // FragmentTag is for storing the fragment which has the api call within Activity Context
    private FragmentTag fragmentTag;

    public SetAttrUserListener(Context context) {
        super(context);
    }

    public SetAttrUserListener(Context context, FragmentTag fragmentTag) {
        super(context);
        this.fragmentTag = fragmentTag;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_SET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateMyChatFilesFolderHandle(request.getMegaStringMap());
                } else {
                    logWarning("Error setting \"My chat files\" folder as user's attribute");
                }
                break;
            case USER_ATTR_FIRSTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateFirstName(context, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_LASTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateLastName(context, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_ALIAS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    String nickname = request.getText();
                    dBH.setContactNickname(nickname, request.getNodeHandle());
                    String message;
                    if (request.getText() == null) {
                        message = context.getString(R.string.snackbar_nickname_removed);
                    } else {
                        message = context.getString(R.string.snackbar_nickname_added);
                    }
                    showSnackbar(context, message);
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    dBH.setContactNickname(null, request.getNodeHandle());
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else {
                    logError("Error adding, updating or removing the alias" + e.getErrorCode());
                }
                break;
            case USER_ATTR_CAMERA_UPLOADS_FOLDER:
                long handle = request.getNodeHandle();
                boolean isSecondary = request.getFlag();
                MegaPreferences prefs = dBH.getPreferences();
                // Database and preference update
                if (e.getErrorCode() == MegaError.API_OK) {
                    if (prefs == null) return;
                    if (isSecondary) {
                        resetSecondaryTimeline();
                        dBH.setSecondaryFolderHandle(handle);
                        prefs.setMegaHandleSecondaryFolder(String.valueOf(handle));
                    } else {
                        resetPrimaryTimeline();
                        dBH.setCamSyncHandle(handle);
                        prefs.setCamSyncHandle(String.valueOf(handle));
                    }
                    forceUpdateCameraUploadFolderIcon(request.getFlag(), request.getNodeHandle());
                }

                if (context instanceof CameraUploadsService) {
                    ((CameraUploadsService) context).onSetFolderAttribute(e.getErrorCode() == MegaError.API_OK);
                } else if (context instanceof ManagerActivityLollipop
                        && fragmentTag == SETTINGS) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        SettingsFragmentLollipop settingsFragment = ((ManagerActivityLollipop) context).getSettingsFragment();
                        if (settingsFragment != null) {
                            settingsFragment.setCUDestinationFolder(isSecondary, handle);
                        }
                        return;
                    }
                    showSnackbar(context, context.getString(R.string.error_unable_to_setup_cloud_folder));
                    logError("Exception happens when set user attribute" + e.toString());
                }
                break;
        }
    }

    /**
     * Updates in DB the handle of "My chat files" folder node if the request
     * for set a node as USER_ATTR_MY_CHAT_FILES_FOLDER finished without errors.
     *
     * Before update the DB, it has to obtain the handle contained in a MegaStringMap,
     * where one of the entries will contain a key "h" and its value, the handle in base64.
     *
     * @param map MegaStringMap which contains the handle of the node set as USER_ATTR_MY_CHAT_FILES_FOLDER.
     */
    private void updateMyChatFilesFolderHandle(MegaStringMap map) {
        if (map != null && map.size() > 0 && !isTextEmpty(map.get("h"))) {
            long handle = base64ToHandle(map.get("h"));
            if (handle != INVALID_HANDLE) {
                MegaApplication.getInstance().getDbH().setMyChatFilesFolderHandle(handle);
            }
        }
    }
}
