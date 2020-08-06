package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.text.Html;
import android.text.Spanned;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.ChatImportToForwardListener;
import mega.privacy.android.app.lollipop.listeners.CopyAndSendToChatListener;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.listeners.MultipleAttachChatListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.lollipop.megachat.NonContactInfo;
import mega.privacy.android.app.utils.DownloadChecker;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.SelectDownloadLocationDialog;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_LEFT_CHAT;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_LEFT_CHAT;
import static mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.*;

public class ChatController {

    private Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;

    public ChatController(Context context){
        logDebug("ChatController created");
        this.context = context;

        if (megaApi == null){
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void leaveChat(MegaChatRoom chat){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            context.sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_LEFT_CHAT)
                    .setAction(ACTION_LEFT_CHAT)
                    .putExtra(CHAT_ID, chat.getChatId()));
            ((GroupChatInfoActivityLollipop) context).finish();
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chat.getChatId(), (ChatActivityLollipop) context);
        }
    }

    public void selectChatsToAttachContact(MegaUser contact){
        logDebug("selectChatsToAttachContact");

        long[] longArray = new long[1];
        longArray[0] = contact.getHandle();

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(USER_HANDLES, longArray);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ((ContactInfoActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public void selectChatsToAttachContacts (ArrayList<MegaUser> contacts) {
        long[] longArray = new long[contacts.size()];

        for (int i=0; i<contacts.size(); i++) {
            longArray[i] = contacts.get(i).getHandle();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(USER_HANDLES, longArray);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public void leaveChat(long chatId){
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.leaveChat(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.leaveChat(chatId, (GroupChatInfoActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.leaveChat(chatId, (ChatActivityLollipop) context);
        }
    }

    public void clearHistory(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());
        clearHistory(chat.getChatId());
    }

    public void clearHistory(long chatId){
        logDebug("Chat ID: " + chatId);
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ManagerActivityLollipop) context);
        }
        else if(context instanceof ChatActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ChatActivityLollipop) context);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (ContactInfoActivityLollipop) context);
        }
        else if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.clearChatHistory(chatId, (GroupChatInfoActivityLollipop) context);
        }

        dbH.removePendingMessageByChatId(chatId);
    }

    public void archiveChat(long chatId){
        logDebug("Chat ID: " + chatId);
        if(context instanceof ManagerActivityLollipop){
            megaChatApi.archiveChat(chatId, true, (ManagerActivityLollipop) context);
        }
    }

    public void archiveChat(MegaChatListItem chatItem){
        logDebug("Chat ID: " + chatItem.getChatId());
        if(context instanceof ManagerActivityLollipop){
            if(chatItem.isArchived()){
                megaChatApi.archiveChat(chatItem.getChatId(), false, (ManagerActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chatItem.getChatId(), true, (ManagerActivityLollipop) context);
            }
        }
        else if(context instanceof ArchivedChatsActivity){
            if(chatItem.isArchived()){
                megaChatApi.archiveChat(chatItem.getChatId(), false, (ArchivedChatsActivity) context);
            }
            else{
                megaChatApi.archiveChat(chatItem.getChatId(), true, (ArchivedChatsActivity) context);
            }
        }
    }

    public void archiveChat(MegaChatRoom chat){
        logDebug("Chat ID: " + chat.getChatId());
        if(context instanceof GroupChatInfoActivityLollipop){

            if(chat.isArchived()){
                megaChatApi.archiveChat(chat.getChatId(), false,(GroupChatInfoActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chat.getChatId(), true, (GroupChatInfoActivityLollipop) context);
            }
        }
        else if(context instanceof ChatActivityLollipop){
            if(chat.isArchived()){
                megaChatApi.archiveChat(chat.getChatId(), false,(ChatActivityLollipop) context);
            }
            else{
                megaChatApi.archiveChat(chat.getChatId(), true, (ChatActivityLollipop) context);
            }
        }
    }

    public void archiveChats(ArrayList<MegaChatListItem> chats){
        logDebug("Chat ID: " + chats.size());
        if(context instanceof ManagerActivityLollipop){
            for(int i=0; i<chats.size(); i++){
                if(chats.get(i).isArchived()){
                    megaChatApi.archiveChat(chats.get(i).getChatId(), false,null);
                }
                else{
                    megaChatApi.archiveChat(chats.get(i).getChatId(), true, null);
                }
            }
        }
        else if(context instanceof ArchivedChatsActivity){
            for(int i=0; i<chats.size(); i++){
                if(chats.get(i).isArchived()){
                    megaChatApi.archiveChat(chats.get(i).getChatId(), false, null);
                }
                else{
                    megaChatApi.archiveChat(chats.get(i).getChatId(), true, null);
                }
            }
        }
    }

    public void deleteMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chat){
        logDebug("Messages to delete: " + messages.size());
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                deleteMessage(messages.get(i), chat.getChatId());
            }
        }
    }

    public void deleteAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chat){
        logDebug("Messages to delete: " + messages.size());
        if(messages!=null){
            for(int i=0; i<messages.size();i++){
                deleteMessage(messages.get(i).getMessage(), chat.getChatId());
            }
        }
    }

    public void deleteMessageById(long messageId, long chatId) {
        logDebug("Message ID: " + messageId + ", Chat ID: " + chatId);
        MegaChatMessage message = getMegaChatMessage(context, megaChatApi, chatId, messageId);

        if(message!=null){
            deleteMessage(message, chatId);
        }
    }

    public void deleteMessage(MegaChatMessage message, long chatId) {
        logDebug("Message : " + message.getMsgId() + ", Chat ID: " + chatId);
        MegaChatMessage messageToDelete;
        if (message == null) return;
        if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.getType() == MegaChatMessage.TYPE_VOICE_CLIP) {
            logDebug("Delete node attachment message or voice clip message");
            if (message.getType() == MegaChatMessage.TYPE_VOICE_CLIP && message.getMegaNodeList() != null && message.getMegaNodeList().size() > 0 && message.getMegaNodeList().get(0) != null) {
                deleteOwnVoiceClip(context, message.getMegaNodeList().get(0).getName());
            }
            megaChatApi.revokeAttachmentMessage(chatId, message.getMsgId());
            return;
        }

        logDebug("Delete normal message with status = "+message.getStatus());
        if(message.getStatus() == MegaChatMessage.STATUS_SENDING && message.getMsgId() == INVALID_HANDLE){

            messageToDelete = megaChatApi.deleteMessage(chatId, message.getTempId());
        }else{
            messageToDelete = megaChatApi.deleteMessage(chatId, message.getMsgId());
        }

        if (messageToDelete == null) {
            logDebug("The message cannot be deleted");
        }else{
            logDebug("The message has been deleted");
            ((ChatActivityLollipop) context).updatingRemovedMessage(message);
        }
    }

    /*
     * Delete a voice note from local storage
     */
    public static void deleteOwnVoiceClip(Context mContext, String nameFile) {
        logDebug("deleteOwnVoiceClip");
        File localFile = buildVoiceClipFile(mContext, nameFile);
        if (!isFileAvailable(localFile)) return;
        localFile.delete();
    }

    public void alterParticipantsPermissions(long chatid, long uh, int privilege){
        logDebug("Chat ID: " + chatid + ", User (uh): " + uh + ", Priv: " + privilege);
        megaChatApi.updateChatPermissions(chatid, uh, privilege, (GroupChatInfoActivityLollipop) context);
    }

    public void removeParticipant(long chatid, long uh){
        logDebug("Chat ID: " + chatid + ", User (uh): " + uh);
        if(context==null){
            logWarning("Context is NULL");
        }
        megaChatApi.removeFromChat(chatid, uh, (GroupChatInfoActivityLollipop) context);
    }

    public void changeTitle(long chatid, String title){
        if(context instanceof GroupChatInfoActivityLollipop){
            megaChatApi.setChatTitle(chatid, title, (GroupChatInfoActivityLollipop) context);
        }
    }

    public void muteChats(ArrayList<MegaChatListItem> chats){
        for(int i=0; i<chats.size();i++){
            muteChat(chats.get(i));
            ((ManagerActivityLollipop)context).showMuteIcon(chats.get(i));
        }
    }

    public void muteChat(long chatHandle){
        logDebug("Chat handle: " + chatHandle);
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(chatHandle));
        if(chatPrefs==null){

            chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(false), "");
            dbH.setChatItemPreferences(chatPrefs);

        }
        else{
            chatPrefs.setNotificationsEnabled(Boolean.toString(false));
            dbH.setNotificationEnabledChatItem(Boolean.toString(false), Long.toString(chatHandle));
        }
    }

    public void muteChat(MegaChatListItem chat){
        logDebug("Chat ID:" + chat.getChatId());
        muteChat(chat.getChatId());
    }

    public void unmuteChats(ArrayList<MegaChatListItem> chats){
        for(int i=0; i<chats.size();i++){
            unmuteChat(chats.get(i));
            ((ManagerActivityLollipop)context).showMuteIcon(chats.get(i));
        }
    }

    public void unmuteChat(MegaChatListItem chat){
        logDebug("Chat ID: " + chat.getChatId());
        unmuteChat(chat.getChatId());
    }

    public void unmuteChat(long chatHandle){
        logDebug("Chant handle: " + chatHandle);
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(chatHandle));
        if(chatPrefs==null){
            chatPrefs = new ChatItemPreferences(Long.toString(chatHandle), Boolean.toString(true), "");
            dbH.setChatItemPreferences(chatPrefs);
        }
        else{
            chatPrefs.setNotificationsEnabled(Boolean.toString(true));
            dbH.setNotificationEnabledChatItem(Boolean.toString(true), Long.toString(chatHandle));
        }
    }

    public String createSingleManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        logDebug("Message ID: " + androidMessage.getMessage().getMsgId() + ", Chat ID: " + chatRoom.getChatId());

        String text = createManagementString(androidMessage.getMessage(), chatRoom);
        if((text!=null) && (!text.isEmpty())){
            text = text.substring(text.indexOf(":")+2);
        }else{
            text = "";

        }
        return text;
    }


    public String createManagementString(MegaChatMessage message, MegaChatRoom chatRoom) {
        logDebug("MessageID: " + message.getMsgId() + ", Chat ID: " + chatRoom.getChatId());
        long userHandle = message.getUserHandle();

        if (message.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
            logDebug("ALTER PARTICIPANT MESSAGE!!");

            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                logDebug("Me alter participant");

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                int privilege = message.getPrivilege();
                logDebug("Privilege of me: " + privilege);
                String textToShow = "";
                String fullNameAction = getParticipantFullName(message.getUserHandle());

                if (privilege != MegaChatRoom.PRIV_RM) {
                    logDebug("I was added");
                    textToShow = String.format(context.getString(R.string.non_format_message_add_participant), megaChatApi.getMyFullname(), fullNameAction);
                } else {
                    logDebug("I was removed or left");
                    if (message.getUserHandle() == message.getHandleOfAction()) {
                        logDebug("I left the chat");
                        textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), megaChatApi.getMyFullname());

                    } else {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), megaChatApi.getMyFullname(), fullNameAction);
                    }
                }

                builder.append(textToShow);
                return builder.toString();

            } else {
                logDebug("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                logDebug("Privilege of the user: " + privilege);

                String fullNameTitle = getParticipantFullName(message.getHandleOfAction());

                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                String textToShow = "";
                if (privilege != MegaChatRoom.PRIV_RM) {
                    logDebug("Participant was added");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        logDebug("By me");
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {
                        logDebug("By other");
                        String fullNameAction = getParticipantFullName(message.getUserHandle());
                        textToShow = String.format(context.getString(R.string.non_format_message_add_participant), fullNameTitle, fullNameAction);
                    }
                }//END participant was added
                else {
                    logDebug("Participant was removed or left");
                    if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                        textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, megaChatApi.getMyFullname());
                    } else {

                        if (message.getUserHandle() == message.getHandleOfAction()) {
                            logDebug("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.non_format_message_participant_left_group_chat), fullNameTitle);

                        } else {
                            logDebug("The participant was removed");
                            String fullNameAction = getParticipantFullName(message.getUserHandle());
                            textToShow = String.format(context.getString(R.string.non_format_message_remove_participant), fullNameTitle, fullNameAction);
                        }
                    }
                } //END participant removed

                builder.append(textToShow);
                return builder.toString();

            } //END CONTACT MANAGEMENT MESSAGE
        }else if (message.getType() == MegaChatMessage.TYPE_PRIV_CHANGE) {
            logDebug("PRIVILEGE CHANGE message");
            if (message.getHandleOfAction() == megaApi.getMyUser().getHandle()) {
                logDebug("A moderator change my privilege");
                int privilege = message.getPrivilege();
                logDebug("Privilege of the user: " + privilege);

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    logDebug("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";

                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    logDebug("I changed my own permission");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, megaChatApi.getMyFullname());
                } else {
                    logDebug("My permission was changed by someone");
                    String fullNameAction = getParticipantFullName(message.getUserHandle());
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), megaChatApi.getMyFullname(), privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            } else {
                logDebug("Participant privilege change!");
                logDebug("Message type PRIVILEGE CHANGE - Message ID: " + message.getMsgId());

                String fullNameTitle = getParticipantFullName(message.getHandleOfAction());
                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                int privilege = message.getPrivilege();
                String privilegeString = "";
                if (privilege == MegaChatRoom.PRIV_MODERATOR) {
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_STANDARD) {
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                } else if (privilege == MegaChatRoom.PRIV_RO) {
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                } else {
                    logDebug("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";
                if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                    logDebug("The privilege was change by me");
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, megaChatApi.getMyFullname());

                } else {
                    logDebug("By other");
                    String fullNameAction = getParticipantFullName(message.getUserHandle());
                    textToShow = String.format(context.getString(R.string.non_format_message_permissions_changed), fullNameTitle, privilegeString, fullNameAction);
                }

                builder.append(textToShow);
                return builder.toString();
            }
        }else {
            logDebug("Other type of messages");
            //OTHER TYPE OF MESSAGES
            if (message.getUserHandle() == megaApi.getMyUser().getHandle()) {
                logDebug("MY message ID: " + message.getMsgId());

                StringBuilder builder = new StringBuilder();
                builder.append(megaChatApi.getMyFullname() + ": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {
                    logDebug("Message type NORMAL");

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        logDebug("Message is edited");
                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        logDebug("Message is deleted");

                        String textToShow = context.getString(R.string.text_deleted_message);
                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();
                    }
                }else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    logDebug("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.history_cleared_by), toCDATA(megaChatApi.getMyFullname()));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    builder.append(result);
                    return builder.toString();
                }else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    logDebug("Message type TITLE CHANGE - Message ID: " + message.getMsgId());

                    String messageContent = message.getContent();
                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), megaChatApi.getMyFullname(), messageContent);
                    builder.append(textToShow);
                    return builder.toString();

                }else if(message.getType() == MegaChatMessage.TYPE_CONTAINS_META){
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if(meta!=null && meta.getType()==MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW){
                       String text = meta.getRichPreview().getText();
                       builder.append(text);
                       return builder.toString();
                    }else{
                       return "";
                    }
                }else if(message.getType() == MegaChatMessage.TYPE_CALL_STARTED){
                    String textToShow = context.getResources().getString(R.string.call_started_messages);
                    builder.append(textToShow);
                    return builder.toString();
                }
                else if(message.getType() == MegaChatMessage.TYPE_CALL_ENDED){
                    String textToShow = "";
                    switch(message.getTermCode()){
                        case MegaChatMessage.END_CALL_REASON_ENDED:{

                            int hours = message.getDuration() / 3600;
                            int minutes = (message.getDuration() % 3600) / 60;
                            int seconds = message.getDuration() % 60;

                            textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                    context.getString(R.string.call_ended_message);

                            if(hours != 0){
                                String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                                textToShow = textToShow + textHours;
                                if((minutes != 0)||(seconds != 0)){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(minutes != 0){
                                String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                                textToShow = textToShow + textMinutes;
                                if(seconds != 0){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(seconds != 0){
                                String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                                textToShow = textToShow + textSeconds;
                            }

                            try{
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                                textToShow = textToShow.replace("[C]", "");
                                textToShow = textToShow.replace("[/C]", "");
                            }catch (Exception e){
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_REJECTED:{

                            textToShow = String.format(context.getString(R.string.call_rejected_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

                            textToShow = String.format(context.getString(R.string.call_not_answered_messages));

                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_FAILED:{

                            textToShow = String.format(context.getString(R.string.call_failed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_CANCELLED:{

                            textToShow = String.format(context.getString(R.string.call_cancelled_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                    }

                    builder.append(textToShow);
                    return builder.toString();
                }
                else{
                    return "";
                }
            } else {
                logDebug("Contact message!!");

                String fullNameTitle = getParticipantFullName(userHandle);
                StringBuilder builder = new StringBuilder();
                builder.append(fullNameTitle + ": ");

                if (message.getType() == MegaChatMessage.TYPE_NORMAL) {

                    String messageContent = "";
                    if (message.getContent() != null) {
                        messageContent = message.getContent();
                    }

                    if (message.isEdited()) {
                        logDebug("Message is edited");

                        String textToShow = messageContent + " " + context.getString(R.string.edited_message_text);
                        builder.append(textToShow);
                        return builder.toString();
                    } else if (message.isDeleted()) {
                        logDebug("Message is deleted");
                        String textToShow = "";
                        if(chatRoom.isGroup()){
                            textToShow = String.format(context.getString(R.string.non_format_text_deleted_message_by), fullNameTitle);
                        }
                        else{
                            textToShow = context.getString(R.string.text_deleted_message);
                        }

                        builder.append(textToShow);
                        return builder.toString();

                    } else {
                        builder.append(messageContent);
                        return builder.toString();

                    }
                } else if (message.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                    logDebug("Message type TRUNCATE");

                    String textToShow = String.format(context.getString(R.string.non_format_history_cleared_by), fullNameTitle);
                    builder.append(textToShow);
                    return builder.toString();

                } else if (message.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
                    logDebug("Message type CHANGE TITLE - Message ID: " + message.getMsgId());

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.non_format_change_title_messages), fullNameTitle, messageContent);
                    builder.append(textToShow);
                    return builder.toString();
                }else if (message.getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                    MegaChatContainsMeta meta = message.getContainsMeta();
                    if(meta!=null && meta.getType()==MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW){
                        String text = meta.getRichPreview().getText();
                        builder.append(text);
                        return builder.toString();
                    }else{
                        return "";
                    }
                }else if(message.getType() == MegaChatMessage.TYPE_CALL_STARTED){
                    String textToShow = context.getResources().getString(R.string.call_started_messages);
                    builder.append(textToShow);
                    return builder.toString();
                }
                else if(message.getType() == MegaChatMessage.TYPE_CALL_ENDED){
                    String textToShow = "";
                    switch(message.getTermCode()){
                        case MegaChatMessage.END_CALL_REASON_ENDED:{

                            int hours = message.getDuration() / 3600;
                            int minutes = (message.getDuration() % 3600) / 60;
                            int seconds = message.getDuration() % 60;

                            textToShow = chatRoom.isGroup() ? context.getString(R.string.group_call_ended_message) :
                                    context.getString(R.string.call_ended_message);

                            if(hours != 0){
                                String textHours = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_hours, hours, hours);
                                textToShow = textToShow + textHours;
                                if((minutes != 0)||(seconds != 0)){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(minutes != 0){
                                String textMinutes = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_minutes, minutes, minutes);
                                textToShow = textToShow + textMinutes;
                                if(seconds != 0){
                                    textToShow = textToShow+", ";
                                }
                            }

                            if(seconds != 0){
                                String textSeconds = context.getResources().getQuantityString(R.plurals.plural_call_ended_messages_seconds, seconds, seconds);
                                textToShow = textToShow + textSeconds;
                            }

                            try{
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                                textToShow = textToShow.replace("[C]", "");
                                textToShow = textToShow.replace("[/C]", "");
                            }catch (Exception e){
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_REJECTED:{

                            textToShow = String.format(context.getString(R.string.call_rejected_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_NO_ANSWER:{

                            textToShow = String.format(context.getString(R.string.call_missed_messages));

                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_FAILED:{

                            textToShow = String.format(context.getString(R.string.call_failed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                        case MegaChatMessage.END_CALL_REASON_CANCELLED:{

                            textToShow = String.format(context.getString(R.string.call_missed_messages));
                            try {
                                textToShow = textToShow.replace("[A]", "");
                                textToShow = textToShow.replace("[/A]", "");
                                textToShow = textToShow.replace("[B]", "");
                                textToShow = textToShow.replace("[/B]", "");
                            } catch (Exception e) {
                            }

                            break;
                        }
                    }

                    builder.append(textToShow);
                    return builder.toString();
                }
                else{
                    logDebug("Message type: " + message.getType());
                    logDebug("Message ID: " + message.getMsgId());
                    return "";
                }
            }
        }
    }

    public String createManagementString(AndroidMegaChatMessage androidMessage, MegaChatRoom chatRoom) {
        logDebug("Message ID: " + androidMessage.getMessage().getMsgId() + ", Chat ID: " + chatRoom.getChatId());

        MegaChatMessage message = androidMessage.getMessage();
        return createManagementString(message, chatRoom);
    }

    /**
     * Gets a partcipant's name (not contact).
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's name.
     */
    private String getNonContactFirstName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String name = nonContact.getFirstName();

        if (isTextEmpty(name)) {
            name = nonContact.getLastName();
        }

        if (isTextEmpty(name)) {
            name = nonContact.getEmail();
        }

        return name;
    }

    /**
     * Gets a partcipant's full name (not contact).
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's full name.
     */
    private String getNonContactFullName(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        if (nonContact == null) {
            return "";
        }

        String fullName = nonContact.getFullName();

        if (isTextEmpty(fullName)) {
            fullName = nonContact.getEmail();
        }

        return fullName;
    }

    /**
     * Gets a partcipant's email (not contact).
     *
     * @param userHandle    participant's identifier
     * @return The participant's email.
     */
    private String getNonContactEmail(long userHandle) {
        NonContactInfo nonContact = dbH.findNonContactByHandle(userHandle + "");
        return nonContact != null ? nonContact.getEmail() : "";
    }

    public String getMyFullName(){

        String fullName = megaChatApi.getMyFullname();

        if(fullName!=null){
            if(fullName.isEmpty()){
                logDebug("Put MY email as fullname");
                String myEmail = megaChatApi.getMyEmail();
                String[] splitEmail = myEmail.split("[@._]");
                fullName = splitEmail[0];
                return fullName;
            }
            else{
                if (fullName.trim().length() <= 0){
                    logDebug("Put MY email as fullname");
                    String myEmail = megaChatApi.getMyEmail();
                    String[] splitEmail = myEmail.split("[@._]");
                    fullName = splitEmail[0];
                    return fullName;
                }
                else{
                    return fullName;
                }
            }
        }
        else{
            logDebug("Put MY email as fullname");
            String myEmail = megaChatApi.getMyEmail();
            String[] splitEmail = myEmail.split("[@._]");
            fullName = splitEmail[0];
            return fullName;
        }
    }

    public void pickFileToSend(){
        logDebug("pickFileToSend");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_MULTISELECT_FILE);
        ((ChatActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    public void saveForOfflineWithMessages(ArrayList<MegaChatMessage> messages, MegaChatRoom chatRoom){
        logDebug("Save for offline multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMegaNodeList(), chatRoom);
        }
    }

    public void saveForOfflineWithAndroidMessages(ArrayList<AndroidMegaChatMessage> messages, MegaChatRoom chatRoom){
        logDebug("Save for offline multiple messages");
        for(int i=0; i<messages.size();i++){
            saveForOffline(messages.get(i).getMessage().getMegaNodeList(), chatRoom);
        }
    }

    public void saveForOffline(MegaNodeList nodeList, MegaChatRoom chatRoom){

        File destination = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                if (context instanceof ChatActivityLollipop) {
                    ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof ChatFullScreenImageViewer){
                    ActivityCompat.requestPermissions(((ChatFullScreenImageViewer) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof PdfViewerActivityLollipop){
                    ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof AudioVideoPlayerLollipop){
                    ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
            }
        }

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        for (int i = 0; i < nodeList.size(); i++) {

            MegaNode document = nodeList.get(i);
            if (document != null) {
                document = authorizeNodeIfPreview(document, chatRoom);
                destination = getOfflineParentFile(context, FROM_OTHERS, document, null);
                destination.mkdirs();

                logDebug("DESTINATION: " + destination.getAbsolutePath());
                if (isFileAvailable(destination) && destination.isDirectory()){

                    File offlineFile = new File(destination, document.getName());
                    if (offlineFile.exists() && document.getSize() == offlineFile.length() && offlineFile.getName().equals(document.getName())){ //This means that is already available offline
                        logWarning("File already exists!");
                        showSnackbar(context, context.getString(R.string.file_already_exists));
                    }
                    else{
                        dlFiles.put(document, destination.getAbsolutePath());
                    }
                }
                else{
                    logError("Destination ERROR");
                }
            }
        }

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(destination.getAbsolutePath());
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if(availableFreeSpace <document.getSize()){
                showErrorAlertDialog(context.getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, ((ChatActivityLollipop) context));
                continue;
            }

            Intent service = new Intent(context, DownloadService.class);
            document = authorizeNodeIfPreview(document, chatRoom);
            String serializeString = document.serialize();
            logDebug("serializeString: " + serializeString);
            service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
            service.putExtra(DownloadService.EXTRA_PATH, path);
            if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof ChatFullScreenImageViewer){
                service.putExtra("fromMV", true);
            }
            context.startService(service);
        }

    }

    public void requestLocalFolder (long size, ArrayList<String> serializedNodes,@Nullable String sdRoot) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select));
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putStringArrayListExtra(FileStorageActivityLollipop.EXTRA_SERIALIZED_NODES, serializedNodes);
        if (sdRoot != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT, sdRoot);
        }

        if(context instanceof ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof ChatFullScreenImageViewer){
            ((ChatFullScreenImageViewer) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }

    private void filePathDefault(String path, final ArrayList<MegaNode> nodeList){
        logDebug("filePathDefault");
        File defaultPathF = new File(path);
        defaultPathF.mkdirs();
        checkSizeBeforeDownload(path, nodeList);
    }

    public void prepareForChatDownload(ArrayList<MegaNodeList> list){
        logDebug("prepareForChatDownload");
        ArrayList<MegaNode> nodeList =  new ArrayList<>();
        MegaNodeList megaNodeList;
        for (int i= 0; i<list.size(); i++){
            megaNodeList = list.get(i);
            for (int j=0; j<megaNodeList.size(); j++){
                nodeList.add(megaNodeList.get(j));
            }
        }
        prepareForDownloadVersions(nodeList);
    }

    public void prepareForChatDownload(MegaNodeList list){
        ArrayList<MegaNode> nodeList = MegaApiJava.nodeListToArray(list);
        prepareForDownloadVersions(nodeList);
    }

    public void prepareForChatDownload(MegaNode node){
        logDebug("Node: " + node.getHandle());
        ArrayList<MegaNode> nodeList = new ArrayList<>();
        nodeList.add(node);
        prepareForDownloadVersions(nodeList);
    }

    private ArrayList<String> serializeNodes(ArrayList<MegaNode> nodeList) {
        ArrayList<String> serializedNodes = new ArrayList<>();
        for (MegaNode node : nodeList) {
            serializedNodes.add(node.serialize());
        }
        return serializedNodes;
    }

    private ArrayList<MegaNode> unSerializeNodes(ArrayList<String> serializedNodes) {
        ArrayList<MegaNode> nodeList = new ArrayList<>();
        if (serializedNodes != null) {
            for (String nodeString : serializedNodes) {
                nodeList.add(MegaNode.unserialize(nodeString));
            }
        }
        return nodeList;
    }

    public void prepareForDownload(Intent intent, String parentPath) {
        ArrayList<String> serializedNodes = intent.getStringArrayListExtra(FileStorageActivityLollipop.EXTRA_SERIALIZED_NODES);
        ArrayList<MegaNode> megaNodes = unSerializeNodes(serializedNodes);
        if (megaNodes.size() > 0) {
            checkSizeBeforeDownload(parentPath, megaNodes);
        }
    }

    private void prepareForDownloadVersions(final ArrayList<MegaNode> nodeList){
        logDebug("Node list size: " + nodeList.size() + " files to download");
        long size = 0;
        for (int i = 0; i < nodeList.size(); i++) {
            size += nodeList.get(i).getSize();
        }
        logDebug("Number of files: " + nodeList.size());

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String downloadLocationDefaultPath = getDownloadLocation();

        if(!nodeList.isEmpty() && isVoiceClip(nodeList.get(0).getName())){
            File vcFile = buildVoiceClipFile(context, nodeList.get(0).getName());
            checkSizeBeforeDownload(vcFile.getParentFile().getPath(), nodeList);
            return;
        }

        boolean askMe = askMe(context);
        if (askMe){
            showSelectDownloadLocationDialog(nodeList, size);
        }
        else{
            logDebug("NOT askMe");
            filePathDefault(downloadLocationDefaultPath,nodeList);
        }
    }

    public void showSelectDownloadLocationDialog(ArrayList<MegaNode> nodeList, long size) {
        logDebug("askMe");
        File[] fs = context.getExternalFilesDirs(null);
        final ArrayList<String> serializedNodes = serializeNodes(nodeList);
        if (fs.length <= 1 || fs[1] == null) {
            requestLocalFolder(size, serializedNodes, null);
        } else {
            SelectDownloadLocationDialog selector = new SelectDownloadLocationDialog(context,SelectDownloadLocationDialog.From.CHAT);
            selector.setChatController(this);
            selector.setSize(size);
            selector.setNodeList(nodeList);
            selector.setSerializedNodes(serializedNodes);
            selector.show();
        }
    }

    public void checkSizeBeforeDownload(String parentPath, ArrayList<MegaNode> nodeList){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        logDebug("Node list size: " + nodeList.size());

        final String parentPathC = parentPath;
        long sizeTemp=0;
        long[] hashes = new long[nodeList.size()];

        for (int i=0;i<nodeList.size();i++) {
            MegaNode node = nodeList.get(i);
            hashes[i] = node.getHandle();
            if(node!=null){
                sizeTemp = sizeTemp+node.getSize();
            }
        }

        final long sizeC = sizeTemp;
        logDebug("The final size is: " + getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = stat.getAvailableBytes();
        } catch (Exception ex) { }

        logDebug("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);
        if(availableFreeSpace < sizeC) {
            showNotEnoughSpaceSnackbar(context);
            logWarning("Not enough space");
            return;
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        if(isVoiceClip(nodeList.get(0).getName())){
            download(parentPath, nodeList);
            return;
        }


        String ask=dbH.getAttributes().getAskSizeDownload();

        if (ask==null) {
            ask="true";
        }

        if (ask.equals("false")) {
            logDebug("SIZE: Do not ask before downloading");
            download(parentPathC, nodeList);
        }
        else{
            logDebug("SIZE: Ask before downloading");
            if (sizeC>104857600) {
                logDebug("Show size confirmation: " + sizeC);
                //Show alert
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof ChatFullScreenImageViewer) {
                    ((ChatFullScreenImageViewer) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof PdfViewerActivityLollipop) {
                    ((PdfViewerActivityLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof AudioVideoPlayerLollipop) {
                    ((AudioVideoPlayerLollipop) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
                else if (context instanceof NodeAttachmentHistoryActivity) {
                    ((NodeAttachmentHistoryActivity) context).askSizeConfirmationBeforeChatDownload(parentPathC, nodeList, sizeC);
                }
            }
            else {
                download(parentPathC, nodeList);
            }
        }
    }

    public void download(String parentPath, ArrayList<MegaNode> nodeList){
        logDebug("download()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                if (context instanceof ManagerActivityLollipop) {
                    ActivityCompat.requestPermissions(((ManagerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }else if (context instanceof ChatFullScreenImageViewer){
                    ActivityCompat.requestPermissions(((ChatFullScreenImageViewer) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }else if (context instanceof ChatActivityLollipop){
                    ActivityCompat.requestPermissions(((ChatActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof PdfViewerActivityLollipop){
                    ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                else if (context instanceof AudioVideoPlayerLollipop){
                    ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context),
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                }
                return;
            }
        }

        if (nodeList == null) {
            return;
        }

        long size = 0;
        for (int i = 0; i < nodeList.size(); i++) {
            size += nodeList.get(i).getSize();
        }

        ArrayList<String> serializedNodes = serializeNodes(nodeList);
        boolean downloadToSDCard = false;
        String downloadRoot = null;
        SDCardOperator sdCardOperator = null;
        if(SDCardOperator.isSDCardPath(parentPath)) {
            DownloadChecker checker = new DownloadChecker(context, parentPath, SelectDownloadLocationDialog.From.CHAT);
            checker.setChatController(this);
            checker.setSize(size);
            checker.setNodeList(nodeList);
            checker.setSerializedNodes(serializedNodes);
            if (checker.check()) {
                downloadRoot = checker.getDownloadRoot();
                downloadToSDCard = downloadRoot != null;
                sdCardOperator = checker.getSdCardOperator();
            } else {
                return;
            }
        }

        if(nodeList.size() == 1) {
            logDebug("hashes.length == 1");
            MegaNode tempNode = nodeList.get(0);
            if (context instanceof ChatActivityLollipop) {
                tempNode = authorizeNodeIfPreview(tempNode, ((ChatActivityLollipop) context).getChatRoom());
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                tempNode = authorizeNodeIfPreview(tempNode, ((NodeAttachmentHistoryActivity) context).getChatRoom());
            }
            if ((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE) {
                logDebug("ISFILE");
                String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize());

                //Check if the file is already downloaded
                MegaApplication app = MegaApplication.getInstance();
                if (localPath != null) {
                    checkDownload(context, tempNode, localPath, parentPath, true, downloadToSDCard, sdCardOperator);

                    if (!Boolean.parseBoolean(dbH.getAutoPlayEnabled()) || isVoiceClip(nodeList.get(0).getName())) {
                        return;
                    }

                    if (MimeTypeList.typeForName(tempNode.getName()).isZip()) {
                        logDebug("MimeTypeList ZIP");
                        File zipFile = new File(localPath);

                        Intent intentZip = new Intent();
                        intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                        intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                        intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                        context.startActivity(intentZip);

                    } else if (MimeTypeList.typeForName(tempNode.getName()).isVideoReproducible() || MimeTypeList.typeForName(tempNode.getName()).isAudio()) {
                        logDebug("Video/Audio file");

                        File mediaFile = new File(localPath);

                        Intent mediaIntent;
                        boolean internalIntent;
                        boolean opusFile = false;
                        if (MimeTypeList.typeForName(tempNode.getName()).isVideoNotSupported() || MimeTypeList.typeForName(tempNode.getName()).isAudioNotSupported()) {
                            mediaIntent = new Intent(Intent.ACTION_VIEW);
                            internalIntent = false;
                            String[] s = tempNode.getName().split("\\.");
                            if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
                                opusFile = true;
                            }
                        } else {
                            internalIntent = true;
                            mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                        }
                        mediaIntent.putExtra(IS_PLAYLIST, false);
                        mediaIntent.putExtra("HANDLE", tempNode.getHandle());
                        mediaIntent.putExtra("adapterType", FROM_CHAT);
                        mediaIntent.putExtra(AudioVideoPlayerLollipop.PLAY_WHEN_READY, app.isActivityVisible());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        } else {
                            mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        }
                        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
                                    intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    intentShare.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (isIntentAvailable(context, intentShare)) {
                                    logDebug("Call to startActivity(intentShare)");
                                    context.startActivity(intentShare);
                                }
                            }
                        }
                    } else if (MimeTypeList.typeForName(tempNode.getName()).isPdf()) {
                        logDebug("Pdf file");

                        File pdfFile = new File(localPath);
                        Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
                        pdfIntent.putExtra("inside", true);
                        pdfIntent.putExtra("HANDLE", tempNode.getHandle());
                        pdfIntent.putExtra("adapterType", FROM_CHAT);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                            pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        } else {
                            pdfIntent.setDataAndType(Uri.fromFile(pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                        }
                        pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(pdfIntent);
                    } else {
                        logDebug("MimeTypeList other file");
                        try {
                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            } else {
                                viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            if (isIntentAvailable(context, viewIntent)) {
                                logDebug("IF isIntentAvailable");
                                context.startActivity(viewIntent);
                            } else {
                                logDebug("ELSE isIntentAvailable");
                                Intent intentShare = new Intent(Intent.ACTION_SEND);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (isIntentAvailable(context, intentShare)) {
                                    logDebug("Call to startActivity(intentShare)");
                                    context.startActivity(intentShare);
                                }
                                showSnackbar(context, context.getString(R.string.general_already_downloaded));
                            }
                        } catch (Exception e) {
                            logWarning("Exception downloading file", e);
                            showSnackbar(context, context.getString(R.string.general_already_downloaded));
                        }
                    }
                    return;
                }
            }
        }

        int numberOfNodesToDownload = 0;
        int numberOfNodesAlreadyDownloaded = 0;
        int numberOfNodesPending = 0;

        for (int i=0; i<nodeList.size();i++) {
            MegaNode nodeToDownload = nodeList.get(i);
            if(nodeToDownload != null){
                logDebug("Node NOT null is going to donwload");
                Map<MegaNode, String> dlFiles = new HashMap<>();
                Map<Long, String> targets = new HashMap<>();

                if (downloadToSDCard) {
                    targets.put(nodeToDownload.getHandle(), parentPath);
                    dlFiles.put(nodeToDownload, downloadRoot);
                } else {
                    dlFiles.put(nodeToDownload, parentPath);
                }

                for (MegaNode document : dlFiles.keySet()) {
                    String path = dlFiles.get(document);
                    String targetPath = targets.get(document.getHandle());

                    if (isTextEmpty(path)) {
                        continue;
                    }

                    numberOfNodesToDownload++;

                    File destDir = new File(path);
                    File destFile;
                    destDir.mkdirs();
                    if (destDir.isDirectory()) {
                        destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName(), destDir.getAbsolutePath() + SEPARATOR));
                    } else {
                        destFile = destDir;
                    }

                    if (isFileAvailable(destFile) && document.getSize() == destFile.length()) {
                        numberOfNodesAlreadyDownloaded++;
                    } else {
                        numberOfNodesPending++;
                        Intent service = new Intent(context, DownloadService.class);
                        if (context instanceof ChatActivityLollipop) {
                            nodeToDownload = authorizeNodeIfPreview(nodeToDownload, ((ChatActivityLollipop) context).getChatRoom());
                        } else if (context instanceof NodeAttachmentHistoryActivity) {
                            nodeToDownload = authorizeNodeIfPreview(nodeToDownload, ((NodeAttachmentHistoryActivity) context).getChatRoom());
                        }
                        String serializeString = nodeToDownload.serialize();

                        if (isVoiceClip(nodeList.get(0).getName())) {
                            service.putExtra(DownloadService.EXTRA_OPEN_FILE, false);
                            service.putExtra(EXTRA_TRANSFER_TYPE, EXTRA_VOICE_CLIP);
                        } else if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof ChatFullScreenImageViewer) {
                            service.putExtra("fromMV", true);
                        }
                        if (downloadToSDCard) {
                            service = NodeController.getDownloadToSDCardIntent(service, path, targetPath, dbH.getSDCardUri());
                        } else {
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                        }
                        logDebug("serializeString: " + serializeString);
                        service.putExtra(DownloadService.EXTRA_SERIALIZE_STRING, serializeString);
                        service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                        context.startService(service);
                    }
                }
            }
        }

        if (numberOfNodesToDownload == 1 && isVoiceClip(nodeList.get(0).getName())) {
            return;
        }

        showSnackBarWhenDownloading(context, numberOfNodesPending, numberOfNodesAlreadyDownloaded);
    }

    public void importNode(long idMessage, long idChat) {
        logDebug("Message ID: " + idMessage + ", Chat ID: " + idChat);
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if(m!=null){
            AndroidMegaChatMessage aMessage = new AndroidMegaChatMessage(m);
            messages.add(aMessage);
            importNodesFromAndroidMessages(messages);
        }
        else{
            logWarning("Message cannot be recovered - null");
        }
    }

    public void importNodesFromMessages(ArrayList<MegaChatMessage> messages){
        logDebug("importNodesFromMessages");

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void importNodesFromAndroidMessages(ArrayList<AndroidMegaChatMessage> messages){
        logDebug("importNodesFromAndroidMessages");

        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);

        long[] longArray = new long[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            longArray[i] = messages.get(i).getMessage().getMsgId();
        }
        intent.putExtra("HANDLES_IMPORT_CHAT", longArray);

        if(context instanceof  ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
        }
    }

    public void prepareMessageToForward(long idMessage, long idChat) {
        logDebug("Message ID: " + idMessage + ", Chat ID: " + idChat);
        ArrayList<MegaChatMessage> messagesSelected = new ArrayList<>();
        MegaChatMessage m = getMegaChatMessage(context, megaChatApi, idChat, idMessage);

        if(m!=null){
            messagesSelected.add(m);
            prepareMessagesToForward(messagesSelected, idChat);
        }
        else{
            logError("Message null");
        }
    }

    public void prepareAndroidMessagesToForward(ArrayList<AndroidMegaChatMessage> androidMessagesSelected, long idChat){
        ArrayList<MegaChatMessage> messagesSelected = new ArrayList<>();

        for(int i = 0; i<androidMessagesSelected.size(); i++){
            messagesSelected.add(androidMessagesSelected.get(i).getMessage());
        }
        prepareMessagesToForward(messagesSelected, idChat);
    }

    public void prepareMessagesToForward(ArrayList<MegaChatMessage> messagesSelected, long idChat){
        logDebug("Number of messages: " + messagesSelected.size() + ",Chat ID: " + idChat);

        ArrayList<MegaChatMessage> messagesToImport = new ArrayList<>();
        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();

            logDebug("Type of message: "+ messagesSelected.get(i).getType());
            if((messagesSelected.get(i).getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT)||(messagesSelected.get(i).getType()==MegaChatMessage.TYPE_VOICE_CLIP)){
                if(messagesSelected.get(i).getUserHandle()!=megaChatApi.getMyUserHandle()){
                    //Node has to be imported
                    messagesToImport.add(messagesSelected.get(i));
                }
            }
        }

        if(messagesToImport.isEmpty()){
            forwardMessages(messagesSelected, idChat);
        }
        else{
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).storedUnhandledData(messagesSelected, messagesToImport);
                ((ChatActivityLollipop) context).handleStoredData();
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                ((NodeAttachmentHistoryActivity) context).storedUnhandledData(messagesSelected, messagesToImport);
                if (existsMyChatFilesFolder()) {
                    ((NodeAttachmentHistoryActivity) context).setMyChatFilesFolder(getMyChatFilesFolder());
                    ((NodeAttachmentHistoryActivity) context).handleStoredData();
                } else {
                    megaApi.getMyChatFilesFolder(new GetAttrUserListener(context));
                }
            }
        }
    }

    public void proceedWithForward(MegaNode myChatFilesFolder, ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport, long idChat) {
        ChatImportToForwardListener listener = new ChatImportToForwardListener(MULTIPLE_FORWARD_MESSAGES, messagesSelected, messagesToImport.size(), context, this, idChat);
        int errors = 0;

        for(int j=0; j<messagesToImport.size();j++){
            MegaChatMessage message = messagesToImport.get(j);

            if(message!=null){

                MegaNodeList nodeList = message.getMegaNodeList();

                for(int i=0;i<nodeList.size();i++){
                    MegaNode document = nodeList.get(i);
                    if (document != null) {
                        logDebug("DOCUMENT: " + document.getHandle());
                        document = authorizeNodeIfPreview(document, megaChatApi.getChatRoom(idChat));
                        megaApi.copyNode(document, myChatFilesFolder, listener);
                    }
                    else{
                        logWarning("DOCUMENT: null");
                    }
                }
            }
            else{
                logWarning("MESSAGE is null");
                errors++;
            }
        }

        if (errors > 0) {
            showSnackbar(context, context.getResources().getQuantityString(R.plurals.messages_forwarded_partial_error, errors, errors));
        }
    }

    public void forwardMessages(ArrayList<MegaChatMessage> messagesSelected, long idChat){
        logDebug("Number of messages: " + messagesSelected.size() + ", Chat ID: " + idChat);

        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMsgId();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra(ID_MESSAGES, idMessages);
        i.putExtra("ID_CHAT_FROM", idChat);
        i.setAction(ACTION_FORWARD_MESSAGES);
        if(context instanceof  ChatActivityLollipop){
            ((ChatActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof  NodeAttachmentHistoryActivity){
            ((NodeAttachmentHistoryActivity) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public MegaNode authorizeNodeIfPreview (MegaNode node, MegaChatRoom chatRoom) {
        if (chatRoom != null && chatRoom.isPreview()) {
            MegaNode nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.getAuthorizationToken());
            if (nodeAuthorized != null) {
                logDebug("Authorized");
                return nodeAuthorized;
            }
        }
        logDebug("NOT authorized");
        return node;
    }

    public boolean isInAnonymousMode () {
        if (megaChatApi.getInitState() == MegaChatApi.INIT_ANONYMOUS) {
            return true;
        }

        return false;
    }

    public boolean isPreview (MegaChatRoom chatRoom) {
        if (chatRoom != null) {
            return chatRoom.isPreview();
        }

        return false;
    }

    public void checkIfNodesAreMineAndAttachNodes(long[] handles, long idChat) {
        long[] idChats = new long[1];
        idChats[0] = idChat;
        checkIfNodesAreMineAndAttachNodes(handles, idChats);
    }

    public void checkIfNodesAreMineAndAttachNodes(long[] handles, long[] idChats) {
        if (handles == null) {
            return;
        }

        MegaNode currentNode;
        ArrayList<MegaNode> nodes = new ArrayList<>();
        ArrayList<MegaNode> ownerNodes = new ArrayList<>();
        ArrayList<MegaNode> notOwnerNodes = new ArrayList<>();
        NodeController nC = new NodeController(context);


        for (int i=0; i<handles.length; i++) {
            currentNode = megaApi.getNodeByHandle(handles[i]);
            if (currentNode != null) {
                nodes.add(currentNode);
            }
        }

        nC.checkIfNodesAreMine(nodes, ownerNodes, notOwnerNodes);

        if (notOwnerNodes.size() == 0) {
            if (context instanceof ContactInfoActivityLollipop) {
                ((ContactInfoActivityLollipop) context).sendFilesToChat(handles, idChats[0]);
                return;
            } else if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).sendFilesToChats(null, idChats, handles);
                return;
            } else if (context instanceof ChatActivityLollipop) {
                MultipleAttachChatListener listener = new MultipleAttachChatListener(context, idChats[0], handles.length);
                for (long fileHandle : handles) {
                    megaChatApi.attachNode(idChats[0], fileHandle, listener);
                }
                return;
            }
        }

        if (context instanceof ContactInfoActivityLollipop || context instanceof ChatActivityLollipop) {
            CopyAndSendToChatListener copyAndSendToChatListener = new CopyAndSendToChatListener(context, idChats[0]);
            copyAndSendToChatListener.copyNodes(notOwnerNodes, ownerNodes);
        } else if (context instanceof ManagerActivityLollipop) {
            CopyAndSendToChatListener copyAndSendToChatListener = new CopyAndSendToChatListener(context, idChats);
            copyAndSendToChatListener.copyNodes(notOwnerNodes, ownerNodes);
        }
    }

    public void checkIntentToShareSomething(Intent intent) {
        long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
        long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
        long[] nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);
        long[] userHandles = intent.getLongArrayExtra(USER_HANDLES);

        if ((chatHandles != null && chatHandles.length > 0) || (contactHandles != null && contactHandles.length > 0)) {
            if (contactHandles != null && contactHandles.length > 0) {
                ArrayList<MegaChatRoom> chats = new ArrayList<>();
                ArrayList<MegaUser> users = new ArrayList<>();

                for (long contactHandle : contactHandles) {
                    MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandle));
                    if (user != null) {
                        users.add(user);
                    }
                }

                if (chatHandles != null) {
                    for (long chatHandle : chatHandles) {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandle);
                        if (chatRoom != null) {
                            chats.add(chatRoom);
                        }
                    }
                }

                CreateChatListener listener = null;
                boolean createChats = false;

                if (nodeHandles != null) {
                    listener = new CreateChatListener(chats, users, nodeHandles, context, CreateChatListener.SEND_FILES, -1);
                    createChats = true;
                } else if (userHandles != null) {
                    listener = new CreateChatListener(chats, users, userHandles, context, CreateChatListener.SEND_CONTACTS, -1);
                    createChats = true;
                } else {
                    logWarning("Error on sending to chat");
                }

                if (createChats) {
                    for (MegaUser user : users) {
                        MegaChatPeerList peers = MegaChatPeerList.createInstance();
                        peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        megaChatApi.createChat(false, peers, listener);
                    }
                }
            } else {
                int countChat = chatHandles.length;
                logDebug("Selected: " + countChat + " chats to send");

                if (nodeHandles != null) {
                    logDebug("Send " + nodeHandles.length + " nodes");
                    checkIfNodesAreMineAndAttachNodes(nodeHandles, chatHandles);
                } else if (userHandles != null) {
                    logDebug("Send " + userHandles.length + " contacts");
                    sendContactsToChats(chatHandles, userHandles);
                } else {
                    logWarning("Error on sending to chat");
                }
            }
        }
    }

    public void sendContactsToChats(long[] chatHandles, long[] userHandles) {
        MegaHandleList handleList = MegaHandleList.createInstance();

        for (long userHandle : userHandles) {
            handleList.addMegaHandle(userHandle);
        }

        for (long chatHandle : chatHandles) {
            megaChatApi.attachContacts(chatHandle, handleList);
        }

        if (chatHandles.length == 1) {
            showSnackbar(context, MESSAGE_SNACKBAR_TYPE, null, chatHandles[0]);
        } else {
            showSnackbar(context, MESSAGE_SNACKBAR_TYPE, null, INVALID_HANDLE);
        }
    }

    /**
     * Method for send a file into one or more chats
     *
     * @param context Context of the Activity where the file has to be sent
     * @param chats Chats where the file has to be sent
     * @param fileHandle Handle of the file that has to be sent
     */
    public static void sendFileToChatsFromContacts(Context context, ArrayList<MegaChatRoom> chats, long fileHandle){
        logDebug("sendFileToChatsFromContacts");

        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        MultipleAttachChatListener listener;

        if(chats.size()==1){
            listener = new MultipleAttachChatListener(context, chats.get(0).getChatId(), chats.size());
            megaChatApi.attachNode(chats.get(0).getChatId(), fileHandle, listener);
        }
        else{
            listener = new MultipleAttachChatListener(context, -1, chats.size());
            for(int i=0;i<chats.size();i++){
                megaChatApi.attachNode(chats.get(i).getChatId(), fileHandle, listener);
            }
        }
    }

    /**
     * Stores in DB the user's attributes of a non contact.
     *
     * @param peerHandle    identifier of the user to save
     */
    public void setNonContactAttributesInDB (long peerHandle) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();

        String firstName = megaChatApi.getUserFirstnameFromCache(peerHandle);
        if (!isTextEmpty(firstName)) {
            dbH.setNonContactFirstName(firstName, peerHandle + "");
        }

        String lastName = megaChatApi.getUserLastnameFromCache(peerHandle);
        if (!isTextEmpty(lastName)) {
            dbH.setNonContactLastName(lastName, peerHandle + "");
        }

        String email = megaChatApi.getUserEmailFromCache(peerHandle);
        if (!isTextEmpty(email)) {
            dbH.setNonContactEmail(email, peerHandle + "");
        }
    }

    /**
     * Gets the participant's first name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     *
     * @param userHandle    participant's identifier
     * @return The participant's first name
     */
    public String getParticipantFirstName(long userHandle) {
        String firstName = getFirstNameDB(userHandle);

        if (isTextEmpty(firstName)) {
            firstName = getNonContactFirstName(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserFirstnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserLastnameFromCache(userHandle);
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserEmailFromCache(userHandle);
        }

        return firstName;
    }

    /**
     * Gets the participant's full name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     *
     * @param handle    participant's identifier
     * @return The participant's full name.
     */
    public String getParticipantFullName(long handle) {
        String fullName = getContactNameDB(handle);

        if (isTextEmpty(fullName)) {
            fullName = getNonContactFullName(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserFullnameFromCache(handle);
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserEmailFromCache(handle);
        }

        return fullName;
    }

    /**
     * Gets the participant's email.
     *
     * @param handle    participant's identifier
     * @return The participant's email.
     */
    public String getParticipantEmail(long handle) {
        String email = getContactEmailDB(handle);

        if (isTextEmpty(email)) {
            email = getNonContactEmail(handle);
        }

        if (isTextEmpty(email)) {
            email = megaChatApi.getUserEmailFromCache(handle);
        }

        return email;
    }
}
