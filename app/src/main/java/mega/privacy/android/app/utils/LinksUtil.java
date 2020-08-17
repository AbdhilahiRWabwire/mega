package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.listeners.SessionTransferURLListener;

import static mega.privacy.android.app.utils.Constants.MEGA_REGEXS;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.matchRegexs;

public class LinksUtil {

    private static final String REQUIRES_TRANSFER_SESSION = "fm/";

    private static boolean isClickAlreadyIntercepted;

    /**
     * Checks if the link received requires transfer session.
     *
     * @param url   link to check
     * @return True if the link requires transfer session, false otherwise.
     */
    public static boolean requiresTransferSession(Context context, String url) {
        if (url.contains(REQUIRES_TRANSFER_SESSION)) {
            int start = url.indexOf(REQUIRES_TRANSFER_SESSION);
            if (start != -1) {
                String path = url.substring(start + REQUIRES_TRANSFER_SESSION.length());
                if (!isTextEmpty(path)) {
                    MegaApplication.getInstance().getMegaApi().getSessionTransferURL(path, new SessionTransferURLListener(context));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the url is a MEGA link and if it requires transfer session.
     *
     * @param context   current Context
     * @param url       link to check
     * @return True if the link is a MEGA link and requires transfer session, false otherwise.
     */
    public static boolean isMEGALinkAndRequiresTransferSession(Context context, String url) {
        return !isTextEmpty(url) && matchRegexs(url, MEGA_REGEXS) && requiresTransferSession(context, url);
    }

    /**
     * Sets a customized onClick listener in a TextView to intercept click events on links:
     * - If the link requires transfer session, requests it.
     * - If not, launches a general ACTION_VIEW intent.
     *
     * @param context       current Context
     * @param strBuilder    SpannableStringBuilder containing the text of the TextView
     * @param span          URLSpan containing the links
     */
    public static void makeLinkClickable(Context context, SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);

        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                isClickAlreadyIntercepted = true;

                String url = span.getURL();
                if (isTextEmpty(url)) return;

                if (!isMEGALinkAndRequiresTransferSession(context, url)) {
                    Uri uri = Uri.parse(url);
                    if (uri == null) {
                        logWarning("Uri is null. Cannot open the link.");
                        return;
                    }

                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            }
        };

        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    /**
     * Checks if the content of the TextView has links.
     * If so, sets a customized onClick listener to intercept the clicks on them.
     *
     * @param context   current Context
     * @param textView  TextView to check
     */
    public static void interceptLinkClicks(Context context, TextView textView) {
        CharSequence sequence = textView.getText();
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(context, strBuilder, span);
        }
        textView.setText(strBuilder);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static boolean isIsClickAlreadyIntercepted() {
        return isClickAlreadyIntercepted;
    }

    public static void resetIsClickAlreadyIntercepted() {
        LinksUtil.isClickAlreadyIntercepted = false;
    }
}
