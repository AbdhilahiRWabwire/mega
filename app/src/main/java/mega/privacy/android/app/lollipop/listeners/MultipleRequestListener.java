package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.showSnackbar;

//Listener for  multiselect
public class MultipleRequestListener implements MegaRequestListenerInterface {

    Context context;

    public MultipleRequestListener(int action, Context context) {
        super();
        this.actionListener = action;
        this.context = context;
    }

    int counter = 0;
    int error = 0;
    int errorBusiness = 0;
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

        logWarning("Counter: " + counter);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        logDebug("Counter: " + counter);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                errorBusiness++;
            }
            error++;
        }
        int requestType = request.getType();
        logDebug("Counter: " + counter);
        logDebug("Error: " + error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0){
            switch (requestType) {
                case  MegaRequest.TYPE_MOVE:{
                    if (actionListener== MULTIPLE_SEND_RUBBISH){
                        logDebug("Move to rubbish request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items-error) + context.getString(R.string.number_incorrectly_moved_to_rubbish, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items);
                        }
                        if(context instanceof ManagerActivityLollipop) {
                            ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                            managerActivity.refreshAfterMovingToRubbish();
                            resetAccountDetailsTimeStamp();
                        }
                        else {
                            ((ContactFileListActivityLollipop) context).refreshAfterMovingToRubbish();
                        }
                    }
                    else if (actionListener== MULTIPLE_RESTORED_FROM_RUBBISH){
                        logDebug("Restore nodes from rubbish request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_restored_from_rubbish, max_items-error) + context.getString(R.string.number_incorrectly_restored_from_rubbish, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_restored_from_rubbish, max_items);
                        }

                        ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                        managerActivity.refreshAfterMovingToRubbish();
                        resetAccountDetailsTimeStamp();
                    }
                    else{
                        logDebug("Move nodes request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved, max_items-error) + context.getString(R.string.number_incorrectly_moved, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved, max_items);
                        }
                        ((ManagerActivityLollipop) context).refreshAfterMoving();
                    }
                    break;
                }
                case MegaRequest.TYPE_REMOVE:{
                    logDebug("Remove multi request finish");
                    if (actionListener==MULTIPLE_LEAVE_SHARE){
                        logDebug("Leave multi share");
                        if(error>0){
                            if (error == errorBusiness) {
                                message = e.getErrorString();
                            } else {
                                message = context.getString(R.string.number_correctly_leaved, max_items - error) + context.getString(R.string.number_no_leaved, error);
                            }
                        }
                        else {
                            message = context.getString(R.string.number_correctly_leaved, max_items);
                        }
                    }
                    else{
                        logDebug("Multi remove");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_removed, max_items-error) + context.getString(R.string.number_no_removed, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_removed, max_items);
                        }

                        if (context instanceof ManagerActivityLollipop) {
                            ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                            managerActivity.refreshAfterRemoving();
                        }
                        resetAccountDetailsTimeStamp();
                    }

                    break;
                }
                case MegaRequest.TYPE_REMOVE_CONTACT:{
                    logDebug("Multi contact remove request finish");
                    if(error>0){
                        message = context.getString(R.string.number_contact_removed, max_items-error) + context.getString(R.string.number_contact_not_removed, error);
                    }
                    else{
                        message = context.getString(R.string.number_contact_removed, max_items);
                    }

                    ((ManagerActivityLollipop) context).updateContactsView(true, false, false);
                    break;
                }
                case MegaRequest.TYPE_COPY:{
                    if (actionListener==MULTIPLE_CONTACTS_SEND_INBOX){
                        logDebug("Send to inbox multiple contacts request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent, max_items-error) + context.getString(R.string.number_no_sent, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent, max_items);
                        }
                    }
                    else if (actionListener==MULTIPLE_FILES_SEND_INBOX){
                        logDebug("Send to inbox multiple files request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items-error) + context.getString(R.string.number_no_sent_multifile, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items);
                        }
                    }
                    else if(actionListener==MULTIPLE_CHAT_IMPORT){
                        //Many files shared with one contacts
                        if(error>0){
                            message = context.getString(R.string.number_correctly_imported_from_chat, max_items-error) + context.getString(R.string.number_no_imported_from_chat, error);
                        }
                        else{
                            message = context.getString(R.string.import_success_message);
                        }
                    }
                    else{
                        logDebug("Copy request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_copied, max_items-error) + context.getString(R.string.number_no_copied, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_copied, max_items);
                        }

                        resetAccountDetailsTimeStamp();
                    }
                    break;
                }
                case MegaRequest.TYPE_INVITE_CONTACT:{

                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
                        logDebug("Remind contact request finished");
                        message = context.getString(R.string.number_correctly_reinvite_contact_request, max_items);
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE){
                        logDebug("Delete contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_delete_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_delete_contact_request, max_items);
                        }
                    }
                    else if (request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
                        logDebug("Invite contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_invite_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_invite_contact_request, max_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REPLY_CONTACT_REQUEST:{
                    logDebug("Multiple reply request sent");

                    if(error>0){
                        message = context.getString(R.string.number_incorrectly_invitation_reply_sent, max_items-error, error);
                    }
                    else{
                        message = context.getString(R.string.number_correctly_invitation_reply_sent, max_items);
                    }
                    break;
                }
                default:
                    break;
            }

            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).removeProgressDialog();
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                ((NodeAttachmentHistoryActivity) context).removeProgressDialog();
            }

            showSnackbar(context, message);
        }
    }
}
