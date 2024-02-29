package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.Constants.AVATAR_PHONE_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_PRIMARY_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_CONTACT_NAME_LAND;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_CONTACT_NAME_PORT;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import mega.privacy.android.app.main.PhoneContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

/*
 * Adapter for FilestorageActivity list
 */
public class PhoneContactsAdapter extends RecyclerView.Adapter<PhoneContactsAdapter.ViewHolderPhoneContacts> implements OnClickListener, SectionTitleProvider {

    @Override
    public String getSectionTitle(int position, Context context) {
        return phoneContacts.get(position).getName().substring(0, 1).toUpperCase();
    }

    private Context mContext;
    MegaApiAndroid megaApi;
    OnItemClickListener mItemClickListener;
    private List<PhoneContactInfo> phoneContacts;
    SparseBooleanArray selectedContacts;

    public PhoneContactsAdapter(Context context, ArrayList<PhoneContactInfo> phoneContacts) {
        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
        setContext(context);
        this.phoneContacts = phoneContacts;
        this.selectedContacts = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    // Set new contacts
    public void setContacts(List<PhoneContactInfo> phoneContacts) {
        this.phoneContacts = phoneContacts;
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {

        if (phoneContacts == null) {
            return 0;
        }

        return phoneContacts.size();

    }

    public PhoneContactInfo getItem(int position) {

        if (position < phoneContacts.size()) {
            return phoneContacts.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class ViewHolderPhoneContacts extends RecyclerView.ViewHolder implements OnClickListener {
        RelativeLayout contactLayout;
        TextView contactNameTextView;
        TextView phoneEmailTextView;
        RoundedImageView imageView;
        RelativeLayout contactImageLayout;
        long contactId;
        String contactName;
        String contactMail;
        int currentPosition;

        public ViewHolderPhoneContacts(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, getPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public ViewHolderPhoneContacts onCreateViewHolder(ViewGroup parentView, int viewType) {

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.contact_explorer_item, parentView, false);
        ViewHolderPhoneContacts holder = new ViewHolderPhoneContacts(rowView);

        holder.contactLayout = rowView.findViewById(R.id.contact_list_item_layout);
        holder.contactNameTextView = rowView.findViewById(R.id.contact_explorer_name);
        if (!isScreenInPortrait(mContext)) {
            holder.contactNameTextView.setMaxWidth(dp2px(MAX_WIDTH_CONTACT_NAME_LAND, mContext.getResources().getDisplayMetrics()));
        } else {
            holder.contactNameTextView.setMaxWidth(dp2px(MAX_WIDTH_CONTACT_NAME_PORT, mContext.getResources().getDisplayMetrics()));
        }

        holder.phoneEmailTextView = rowView.findViewById(R.id.contact_explorer_phone_mail);
        holder.imageView = rowView.findViewById(R.id.contact_explorer_thumbnail);
        holder.contactImageLayout = rowView.findViewById(R.id.contact_explorer_relative_layout_avatar);

        return holder;

    }

    @Override
    public void onBindViewHolder(ViewHolderPhoneContacts holder, int position) {

        PhoneContactInfo contact = getItem(position);

        holder.currentPosition = holder.getBindingAdapterPosition();
        holder.contactMail = contact.getEmail();
        holder.contactName = contact.getName();
        holder.contactId = contact.getId();

        holder.contactNameTextView.setText(contact.getName());
        holder.phoneEmailTextView.setText(contact.getEmail());

        createDefaultAvatar(holder, false);

        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(mContext.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(holder.contactId)));

            if (inputStream != null) {
                Bitmap photo = BitmapFactory.decodeStream(inputStream);
                holder.imageView.setImageBitmap(photo);
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createDefaultAvatar(ViewHolderPhoneContacts holder, boolean isMegaContact) {
        Timber.d("isMegaContact: %s", isMegaContact);

        int color;
        if (isMegaContact) {
            color = getSpecificAvatarColor(AVATAR_PRIMARY_COLOR);
        } else {
            color = getSpecificAvatarColor(AVATAR_PHONE_COLOR);
        }

        String name = null;
        if (isMegaContact && holder.contactMail != null && holder.contactMail.length() > 0) {
            name = holder.contactMail;
        } else if (!isMegaContact && holder.contactName != null && holder.contactName.length() > 0) {
            name = holder.contactName;
        }

        Bitmap bitmap = getDefaultAvatar(color, name, AVATAR_SIZE, true);
        holder.imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onClick(View v) {
        Timber.d("click!");
    }
}
