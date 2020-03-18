package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import com.vdurmont.emoji.EmojiParser;

import java.io.File;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ShareContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;
import static mega.privacy.android.app.utils.Util.*;

public class AvatarUtil {

    /*Method to get the letter or emoji, which will be painted in the default avatar*/
    public static String getFirstLetter(String text) {
        String resultUnknown = String.valueOf(UNKNOWN_USER_NAME_AVATAR.charAt(0)).toUpperCase(Locale.getDefault());
        if (text.isEmpty()) {
            return resultUnknown;
        }

        text = text.trim();
        if (text.length() == 1) {
            return String.valueOf(text.charAt(0)).toUpperCase(Locale.getDefault());
        }

        String resultTitle = EmojiUtilsShortcodes.emojify(text);
        List<EmojiRange> emojis = EmojiManager.getInstance().findAllEmojis(resultTitle);

        if (emojis != null && emojis.size() > 0 && emojis.get(0).start == 0) {
            String megaEmoji = resultTitle.substring(emojis.get(0).start, emojis.get(0).end);
            return megaEmoji;
        }

        String resultEmojiCompat = hasEmojiCompatAtFirst(resultTitle);
        if (resultEmojiCompat != null) {
            return resultEmojiCompat;
        }

        String resultChar = String.valueOf(resultTitle.charAt(0)).toUpperCase(Locale.getDefault());
        if (resultChar == null || resultChar.trim().isEmpty() || resultChar.equals("(") || !isRecognizableCharacter(resultChar.charAt(0))) {
            return resultUnknown;
        }

        return resultChar;
    }

    private static boolean isRecognizableCharacter(char input_char) {
        return (input_char >= 48 && input_char <= 57) || (input_char >= 65 && input_char <= 90) || (input_char >= 97 && input_char <= 122);
    }

    private static String hasEmojiCompatAtFirst(String text) {
        List<String> listEmojis = EmojiParser.extractEmojis(text);
        if (listEmojis != null && !listEmojis.isEmpty()) {
            String substring = text.substring(0, listEmojis.get(0).length());
            List<String> sublistEmojis = EmojiParser.extractEmojis(substring);
            if (sublistEmojis != null && !sublistEmojis.isEmpty()) {
                return substring;
            }
        }
        return null;
    }

    /*Methods to obtain the color of the avatar*/
    public static int getColorAvatar(Context context, MegaApiAndroid megaApi, MegaUser user) {
        if(user == null)return getColorAvatar(context, null);

        return getColorAvatar(context, megaApi.getUserAvatarColor(user));
    }

    public static int getColorAvatar(Context context, MegaApiAndroid megaApi, long handle) {
        if(handle == -1 ) return getColorAvatar(context, null);

        return getColorAvatar(context, megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(handle)));
    }

    public static int getColorAvatar(Context context, MegaApiAndroid megaApi, String handle) {
        if(handle == null ) return getColorAvatar(context, null);

        return getColorAvatar(context, megaApi.getUserAvatarColor(handle));
    }

    private static int getColorAvatar(Context context, String color){
        if(color == null) return ContextCompat.getColor(context, R.color.lollipop_primary_color);
        return Color.parseColor(color);
    }

    /*Method to get the default avatar bitmap*/
    public static Bitmap getDefaultAvatar(Context context, int colorAvatar, String textAvatar, int textSize, boolean isList, boolean customEmojis) {
        Bitmap defaultAvatar = Bitmap.createBitmap(DEFAULT_AVATAR_WIDTH_HEIGHT, DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);

        /*Background*/
        Paint paintCircle = new Paint();
        paintCircle.setColor(colorAvatar);
        paintCircle.setAntiAlias(true);

        if(isList){
            /*Shape list*/
            int radius;
            if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
                radius = defaultAvatar.getWidth() / 2;
            }else {
                radius = defaultAvatar.getHeight() / 2;
            }
            c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, paintCircle);
        }else{
            /*Shape grid*/
            Path path = getRoundedRect(0, 0, DEFAULT_AVATAR_WIDTH_HEIGHT , DEFAULT_AVATAR_WIDTH_HEIGHT, 10, 10,true, true, false, false);
            c.drawPath(path, paintCircle);
        }

        /*Text*/
        Typeface face = Typeface.SANS_SERIF;
        Paint paintText = new Paint();
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(textSize);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        /*First Letter*/
        if (textAvatar == null || textAvatar.trim().length() <= 0) {
            textAvatar = UNKNOWN_USER_NAME_AVATAR;
        }

        String firstLetter = getFirstLetter(textAvatar);
        if (customEmojis && EmojiManager.getInstance().getFirstEmoji(firstLetter) != null) {
            Bitmap emojiBitmap = Bitmap.createScaledBitmap(EmojiManager.getInstance().getFirstEmoji(firstLetter).getBitmap(context), textSize, textSize, false);
            int xPos = (c.getWidth() - emojiBitmap.getWidth()) / 2;
            int yPos = (c.getHeight() - emojiBitmap.getHeight()) / 2;
            c.drawBitmap(emojiBitmap, xPos, yPos, paintText);
        } else {
            Rect bounds = new Rect();
            paintText.getTextBounds(firstLetter, 0, firstLetter.length(), bounds);
            int xPos = (c.getWidth() / 2);
            int yPos = (int) ((c.getHeight() / 2) - ((paintText.descent() + paintText.ascent() / 2)) + 20);
            c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);
        }
        return defaultAvatar;
    }

    /*Method to get the default avatar bitmap with custom emojis*/
    public static Bitmap getDefaultAvatar(Context context, int colorAvatar, String textAvatar, int textSize, boolean isList) {
        return getDefaultAvatar(context, colorAvatar, textAvatar, textSize, isList, true);
    }

    /*Methods to get the default avatar bitmap from a Share Contact*/
    public static Bitmap getAvatarShareContact(Context context, MegaApiAndroid megaApi, ShareContactInfo contact){
        String mail = ((AddContactActivityLollipop) context).getShareContactMail(contact);
        int color;
        if (contact.isPhoneContact()) {
            color = ContextCompat.getColor(context, R.color.color_default_avatar_phone);
        } else if (contact.isMegaContact()) {
            color = getColorAvatar(context, megaApi, contact.getMegaContactAdapter().getMegaUser());
        } else {
            color = getColorAvatar(context, null);
        }

        String fullName = null;
        if(contact.isPhoneContact()){
            fullName = contact.getPhoneContactInfo().getName();
        } else if (contact.isMegaContact()) {
            fullName = contact.getMegaContactAdapter().getFullName();
        }
        if (fullName == null) {
            fullName = mail;
        }

        if(contact.isPhoneContact() || contact.isMegaContact()) {
            /*Avatar*/
            File avatar = buildAvatarFile(context, mail + ".jpg");
            Bitmap bitmap;
            if (isFileAvailable(avatar) && avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap != null) {
                    return getCircleBitmap(bitmap);
                }
            }
        }

        /*Default Avatar*/
        return getDefaultAvatar(context, color, fullName, AVATAR_SIZE, true);
    }

}
