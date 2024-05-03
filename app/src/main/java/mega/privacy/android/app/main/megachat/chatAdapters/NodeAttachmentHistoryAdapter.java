package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.FileUtil.isVideoFile;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbAndSetViewForList;
import static mega.privacy.android.app.utils.ThumbnailUtils.getThumbAndSetViewOrCreateForList;
import static mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.formatDateAndTime;
import static mega.privacy.android.app.utils.TimeUtils.getVideoDuration;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class NodeAttachmentHistoryAdapter extends RecyclerView.Adapter<NodeAttachmentHistoryAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener {

    public static final int ITEM_VIEW_TYPE_LIST = 0;
    public static final int ITEM_VIEW_TYPE_GRID = 1;

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    ArrayList<MegaChatMessage> messages;

    Object fragment;
    long parentHandle = -1;
    DisplayMetrics outMetrics;

    private SparseBooleanArray selectedItems;

    RecyclerView listFragment;

    boolean multipleSelect;

    ChatController cC;

    int adapterType;

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView savedOffline;
        public ImageView publicLinkImage;
        public TextView textViewFileName;
        public EmojiTextView textViewMessageInfo;
        public long document;
        public RelativeLayout itemLayout;
        String fullNameTitle;
        boolean nameRequestedAction = false;
    }

    public static class ViewHolderBrowserList extends NodeAttachmentHistoryAdapter.ViewHolderBrowser {

        public ViewHolderBrowserList(View v) {
            super(v);
        }

        public ImageView imageView;
        public RelativeLayout threeDotsLayout;
        public ImageView versionsIcon;
        ImageView threeDotsImageView;
    }

    public static class ViewHolderBrowserGrid extends NodeAttachmentHistoryAdapter.ViewHolderBrowser {

        public ViewHolderBrowserGrid(View v) {
            super(v);
        }

        public ImageView imageViewThumb;
        public ImageView imageViewIcon;
        public RelativeLayout thumbLayout;
        public ImageView imageViewVideoIcon;
        public TextView videoDuration;
        public RelativeLayout videoInfoLayout;
        public ImageButton imageButtonThreeDots;

        public View fileLayout;
        public RelativeLayout thumbLayoutForFile;
        public ImageView fileGridIconForFile;
        public ImageButton imageButtonThreeDotsForFile;
        public TextView textViewFileNameForFile;
        public RadioButton fileGridSelected;
    }

    public void toggleAllSelection(int pos) {
        Timber.d("position: %s", pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);

        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        if (adapterType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST) {
            Timber.d("Adapter type is LIST");
            NodeAttachmentHistoryAdapter.ViewHolderBrowserList view = (NodeAttachmentHistoryAdapter.ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                Timber.d("Start animation: %d multiselection state: %s", pos, isMultipleSelect());
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        Timber.d("onAnimationEnd: %s", selectedItems.size());
                        if (selectedItems.size() <= 0) {
                            Timber.d("toggleAllSelection: hideMultipleSelect");

                            ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
                        }
                        Timber.d("toggleAllSelection: notified item changed");
                        notifyItemChanged(positionToflip);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.imageView.startAnimation(flipAnimation);
            } else {
                Timber.w("NULL view pos: %s", positionToflip);
                notifyItemChanged(pos);
            }
        } else {
            Timber.d("Adapter type is GRID");
            if (selectedItems.size() <= 0) {
                ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
            }
            notifyItemChanged(positionToflip);
        }
    }

    public void toggleSelection(int pos) {
        Timber.d("position: %s", pos);

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
        if (adapterType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST) {
            Timber.d("Adapter type is LIST");
            NodeAttachmentHistoryAdapter.ViewHolderBrowserList view = (NodeAttachmentHistoryAdapter.ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
            if (view != null) {
                Timber.d("Start animation: %s", pos);
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (selectedItems.size() <= 0) {
                            ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                view.imageView.startAnimation(flipAnimation);

            } else {
                Timber.w("View is null - not animation");
                if (selectedItems.size() <= 0) {
                    ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
                }
            }
        } else {
            Timber.d("Adapter type is GRID");

            if (selectedItems.size() <= 0) {
                ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
            }
        }
    }

    public void selectAll() {
        for (int i = 0; i < messages.size(); i++) {
            if (!isItemChecked(i)) {
                //Exclude placeholder.
                if (messages.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        for (int i = 0; i < messages.size(); i++) {
            if (isItemChecked(i)) {
                //Exclude placeholder.
                if (messages.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    /*
     * Get list of all selected messages
     */
    public ArrayList<MegaChatMessage> getSelectedMessages() {
        ArrayList<MegaChatMessage> messages = new ArrayList<MegaChatMessage>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                MegaChatMessage message = getMessageAt(selectedItems.keyAt(i));
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public NodeAttachmentHistoryAdapter(Context _context, ArrayList<MegaChatMessage> _messages, RecyclerView recyclerView, int adapterType) {

        this.context = _context;
        this.messages = _messages;
        this.adapterType = adapterType;

        this.listFragment = recyclerView;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        cC = new ChatController(context);
    }

    public void setMessages(ArrayList<MegaChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public NodeAttachmentHistoryAdapter.ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (viewType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST) {
            Timber.d("Type: ITEM_VIEW_TYPE_LIST");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
            ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
            holderList.itemLayout = v.findViewById(R.id.file_list_item_layout);
            holderList.imageView = v.findViewById(R.id.file_list_thumbnail);
            holderList.savedOffline = v.findViewById(R.id.file_list_saved_offline);
            holderList.publicLinkImage = v.findViewById(R.id.file_list_public_link);
            holderList.textViewFileName = v.findViewById(R.id.file_list_filename);
            holderList.textViewMessageInfo = v.findViewById(R.id.file_list_filesize);
            holderList.threeDotsLayout = v.findViewById(R.id.file_list_three_dots_layout);
            holderList.threeDotsImageView = v.findViewById(R.id.file_list_three_dots);
            holderList.versionsIcon = v.findViewById(R.id.file_list_versions_icon);
            holderList.textViewMessageInfo.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams paramsThreeDotsIcon = (RelativeLayout.LayoutParams) holderList.threeDotsImageView.getLayoutParams();
            paramsThreeDotsIcon.leftMargin = scaleWidthPx(8, outMetrics);
            holderList.threeDotsImageView.setLayoutParams(paramsThreeDotsIcon);

            holderList.textViewMessageInfo.setSelected(true);
            holderList.textViewMessageInfo.setHorizontallyScrolling(true);
            holderList.textViewMessageInfo.setFocusable(true);
            holderList.textViewMessageInfo.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            holderList.textViewMessageInfo.setMarqueeRepeatLimit(-1);
            holderList.textViewMessageInfo.setSingleLine(true);
            holderList.textViewMessageInfo.setHorizontallyScrolling(true);

            holderList.savedOffline.setVisibility(View.INVISIBLE);
            holderList.versionsIcon.setVisibility(View.GONE);
            holderList.publicLinkImage.setVisibility(View.GONE);

            holderList.itemLayout.setTag(holderList);
            holderList.itemLayout.setOnClickListener(this);
            holderList.itemLayout.setOnLongClickListener(this);

            holderList.threeDotsLayout.setTag(holderList);
            holderList.threeDotsLayout.setOnClickListener(this);

            v.setTag(holderList);
            return holderList;
        } else if (viewType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_GRID) {
            Timber.d("Type: ITEM_VIEW_TYPE_GRID");

            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_grid, parent, false);
            NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid holderGrid = new NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid(v);

            holderGrid.fileLayout = v.findViewById(R.id.item_file_grid_file);
            holderGrid.itemLayout = v.findViewById(R.id.file_grid_item_layout);
            holderGrid.imageViewThumb = v.findViewById(R.id.file_grid_thumbnail);
            holderGrid.imageViewIcon = v.findViewById(R.id.file_grid_icon);
            holderGrid.fileGridIconForFile = v.findViewById(R.id.file_grid_icon_for_file);
            holderGrid.thumbLayout = v.findViewById(R.id.file_grid_thumbnail_layout);
            holderGrid.thumbLayoutForFile = v.findViewById(R.id.file_grid_thumbnail_layout_for_file);
            holderGrid.textViewFileName = v.findViewById(R.id.file_grid_filename);
            holderGrid.textViewFileNameForFile = v.findViewById(R.id.file_grid_filename_for_file);
            holderGrid.imageButtonThreeDotsForFile = v.findViewById(R.id.file_grid_three_dots_for_file);
            holderGrid.imageButtonThreeDots = v.findViewById(R.id.file_grid_three_dots);

            holderGrid.imageViewVideoIcon = v.findViewById(R.id.file_grid_video_icon);
            holderGrid.videoDuration = v.findViewById(R.id.file_grid_title_video_duration);
            holderGrid.videoInfoLayout = v.findViewById(R.id.item_file_videoinfo_layout);
            holderGrid.fileGridSelected = v.findViewById(R.id.file_grid_radio_button);

            holderGrid.itemLayout.setTag(holderGrid);
            holderGrid.itemLayout.setOnClickListener(this);
            holderGrid.itemLayout.setOnLongClickListener(this);

            holderGrid.imageButtonThreeDots.setTag(holderGrid);
            holderGrid.imageButtonThreeDots.setOnClickListener(this);
            holderGrid.imageButtonThreeDotsForFile.setTag(holderGrid);
            holderGrid.imageButtonThreeDotsForFile.setOnClickListener(this);
            v.setTag(holderGrid);

            return holderGrid;
        } else {
            return null;
        }
    }

    public void onBindViewHolder(NodeAttachmentHistoryAdapter.ViewHolderBrowser holder, int position) {
        Timber.d("position: %s", position);

        if (adapterType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST) {
            NodeAttachmentHistoryAdapter.ViewHolderBrowserList holderList = (NodeAttachmentHistoryAdapter.ViewHolderBrowserList) holder;
            onBindViewHolderList(holderList, position);
        } else if (adapterType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_GRID) {
            NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid holderGrid = (NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder;
            onBindViewHolderGrid(holderGrid, position);
        }
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position) {
        Timber.d("position: %s", position);
        MegaChatMessage m = (MegaChatMessage) getItem(position);
        MegaNode node = m.getMegaNodeList().get(0);

        holder.document = node.getHandle();
        Bitmap thumb = null;

        Timber.d("Node : %d %s", position, node.getName());

        holder.textViewFileName.setText(node.getName());
        holder.videoInfoLayout.setVisibility(View.GONE);

        holder.itemLayout.setVisibility(View.VISIBLE);

        holder.imageViewThumb.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
        holder.imageViewThumb.setVisibility(View.GONE);
        holder.fileLayout.setVisibility(View.VISIBLE);
        holder.textViewFileName.setVisibility(View.VISIBLE);

        holder.textViewFileNameForFile.setText(node.getName());

        holder.fileGridIconForFile.setVisibility(View.VISIBLE);
        holder.fileGridIconForFile.setImageResource(MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
        holder.thumbLayoutForFile.setBackgroundColor(Color.TRANSPARENT);

        if (multipleSelect && isItemChecked(position)) {
            holder.itemLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.background_item_grid_selected));
            holder.fileGridSelected.setVisibility(View.VISIBLE);

        } else {
            holder.itemLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.background_item_grid));
            holder.fileGridSelected.setVisibility(View.GONE);
        }

        if (isVideoFile(node.getName())) {
            holder.videoInfoLayout.setVisibility(View.VISIBLE);
            holder.videoDuration.setVisibility(View.GONE);
            Timber.d("%s DURATION: %d", node.getName(), node.getDuration());
            int duration = node.getDuration();
            if (duration > 0) {
                holder.videoDuration.setText(getVideoDuration(duration));
                holder.videoDuration.setVisibility(View.VISIBLE);
            }
        }

        if (node.hasThumbnail()) {

            Bitmap temp = ThumbnailUtils.getThumbnailFromCache(node);

            if (temp != null) {
                thumb = ThumbnailUtils.getRoundedRectBitmap(context, temp, 2);
                holder.fileGridIconForFile.setVisibility(View.GONE);
                holder.imageViewThumb.setVisibility(View.VISIBLE);
                holder.imageViewThumb.setImageBitmap(thumb);
                holder.thumbLayoutForFile.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));

            } else {
                temp = ThumbnailUtils.getThumbnailFromFolder(node, context);

                if (temp != null) {
                    thumb = ThumbnailUtils.getRoundedRectBitmap(context, temp, 2);
                    holder.fileGridIconForFile.setVisibility(View.GONE);
                    holder.imageViewThumb.setVisibility(View.VISIBLE);
                    holder.imageViewThumb.setImageBitmap(thumb);
                    holder.thumbLayoutForFile.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));

                } else {
                    try {
                        temp = ThumbnailUtils.getThumbnailFromMegaGrid(node, context, holder, megaApi, this);

                    } catch (Exception e) {
                    } // Too many AsyncTasks

                    if (temp != null) {
                        thumb = ThumbnailUtils.getRoundedRectBitmap(context, temp, 2);
                        holder.imageViewIcon.setVisibility(View.GONE);
                        holder.imageViewThumb.setVisibility(View.VISIBLE);
                        holder.imageViewThumb.setImageBitmap(thumb);
                        holder.thumbLayoutForFile.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
                    }
                }
            }
        } else {
            Bitmap temp = ThumbnailUtils.getThumbnailFromCache(node);

            if (temp != null) {
                thumb = ThumbnailUtils.getRoundedRectBitmap(context, temp, 2);
                holder.fileGridIconForFile.setVisibility(View.GONE);
                holder.imageViewThumb.setVisibility(View.VISIBLE);
                holder.imageViewThumb.setImageBitmap(thumb);
                holder.thumbLayoutForFile.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
            } else {
                temp = ThumbnailUtils.getThumbnailFromFolder(node, context);

                if (temp != null) {
                    thumb = ThumbnailUtils.getRoundedRectBitmap(context, temp, 2);
                    holder.fileGridIconForFile.setVisibility(View.GONE);
                    holder.imageViewThumb.setVisibility(View.VISIBLE);
                    holder.imageViewThumb.setImageBitmap(thumb);
                    holder.thumbLayoutForFile.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
                } else {
                    try {
                        ThumbnailUtils.createThumbnailGrid(context, node, holder, megaApi, this);
                    } catch (Exception e) {
                    } // Too many AsyncTasks
                }
            }
        }
    }

    public void onBindViewHolderList(ViewHolderBrowserList holder, int position) {
        Timber.d("position: %s", position);
        MegaChatMessage m = (MegaChatMessage) getItem(position);
        MegaNode node = m.getMegaNodeList().get(0);

        holder.document = node.getHandle();
        Bitmap thumb = null;

        holder.textViewFileName.setText(node.getName());
        holder.textViewMessageInfo.setText("");

        String date = formatDateAndTime(context, m.getTimestamp(), DATE_LONG_FORMAT);

        if (m.getUserHandle() == megaChatApi.getMyUserHandle()) {
            Timber.d("MY message handle!!: %s", m.getMsgId());
            holder.fullNameTitle = megaChatApi.getMyFullname();
        } else {

            long userHandle = m.getUserHandle();
            Timber.d("Contact message!!: %s", userHandle);

            if (((NodeAttachmentHistoryActivity) context).chatRoom.isGroup()) {

                holder.fullNameTitle = cC.getParticipantFullName(userHandle);

                if (holder.fullNameTitle == null) {
                    holder.fullNameTitle = "";
                }

                if (holder.fullNameTitle.trim().length() <= 0) {

                    Timber.w("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    holder.fullNameTitle = context.getString(R.string.unknown_name_label);
                    if (!(holder.nameRequestedAction)) {
                        Timber.d("Call for nonContactName: %s", m.getUserHandle());
                        holder.nameRequestedAction = true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.isPreview());
                        megaChatApi.getUserFirstname(userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserLastname(userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserEmail(userHandle, listener);
                    } else {
                        Timber.w("Name already asked and no name received: %s", m.getUserHandle());
                    }
                }

            } else {
                holder.fullNameTitle = getTitleChat(((NodeAttachmentHistoryActivity) context).chatRoom);
            }
        }

        String secondRowInfo = context.getString(R.string.second_row_info_item_shared_file_chat, holder.fullNameTitle, date);

        holder.textViewMessageInfo.setText(secondRowInfo);
        holder.textViewMessageInfo.setVisibility(View.VISIBLE);

        if (!multipleSelect) {
            holder.threeDotsLayout.setVisibility(View.VISIBLE);
            holder.threeDotsLayout.setOnClickListener(this);
            Timber.d("Not multiselect");
            holder.itemLayout.setBackground(null);
            holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
            params.setMargins(0, 0, 0, 0);
            holder.imageView.setLayoutParams(params);

            Timber.d("Check the thumb");

            if (node.hasThumbnail()) {
                Timber.d("Node has thumbnail");
                getThumbAndSetView(holder, node);
            } else {
                Timber.d("Node NOT thumbnail");
                getThumbAndSetViewOrCreate(holder, node);
            }
        } else {
            holder.threeDotsLayout.setOnClickListener(null);
            holder.threeDotsLayout.setVisibility(View.GONE);
            Timber.d("Multiselection ON");
            if (this.isItemChecked(position)) {
                RelativeLayout.LayoutParams paramsMultiselect = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
                paramsMultiselect.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
                paramsMultiselect.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
                paramsMultiselect.setMargins(0, 0, 0, 0);
                holder.imageView.setLayoutParams(paramsMultiselect);
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
            } else {
                Timber.d("Check the thumb");
                holder.itemLayout.setBackground(null);
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail");
                    getThumbAndSetView(holder, node);
                } else {
                    Timber.d("Node NOT thumbnail");
                    getThumbAndSetViewOrCreate(holder, node);
                }
            }
        }
    }

    private void getThumbAndSetView(ViewHolderBrowserList holder, MegaNode node) {
        getThumbAndSetViewForList(context, node, holder, megaApi, this, holder.imageView);
    }

    private void getThumbAndSetViewOrCreate(ViewHolderBrowserList holder, MegaNode node) {
        getThumbAndSetViewOrCreateForList(context, node, holder, megaApi, this, holder.imageView);
    }

    @Override
    public int getItemCount() {
        if (messages != null) {
            return messages.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return adapterType;
    }

    public Object getItem(int position) {
        if (messages != null) {
            return messages.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();

        Timber.d("Current position: %s", currentPosition);

        if (currentPosition < 0) {
            Timber.w("Current position error - not valid value");
            return;
        }

        final MegaChatMessage m = (MegaChatMessage) getItem(currentPosition);
        if (m == null) {
            return;
        }
        int id = v.getId();
        if (id == R.id.file_list_three_dots_layout || id == R.id.file_grid_three_dots) {
            threeDotsClicked(currentPosition, m);
        } else if (id == R.id.file_grid_three_dots_for_file) {
            threeDotsClicked(currentPosition, m);
        } else if (id == R.id.file_list_item_layout || id == R.id.file_grid_item_layout) {
            int[] screenPosition = new int[2];
            ImageView imageView;
            if (adapterType == NodeAttachmentHistoryAdapter.ITEM_VIEW_TYPE_LIST) {
                imageView = v.findViewById(R.id.file_list_thumbnail);
            } else {
                imageView = v.findViewById(R.id.file_grid_thumbnail);
            }
            imageView.getLocationOnScreen(screenPosition);

            int[] dimens = new int[4];
            dimens[0] = screenPosition[0];
            dimens[1] = screenPosition[1];
            dimens[2] = imageView.getWidth();
            dimens[3] = imageView.getHeight();

            ((NodeAttachmentHistoryActivity) context).itemClick(currentPosition);
        }
    }

    public void loadPreviousMessages(ArrayList<MegaChatMessage> messages, int counter) {
        Timber.d("counter: %s", counter);
        this.messages = messages;
        notifyItemRangeInserted(messages.size() - counter, counter);
    }

    public void addMessage(ArrayList<MegaChatMessage> messages, int position) {
        Timber.d("position: %s", position);
        this.messages = messages;
        notifyItemInserted(position);
        if (position == messages.size()) {
            Timber.d("No need to update more");
        } else {
            int itemCount = messages.size() - position;
            Timber.d("Update until end - itemCount: %s", itemCount);
            notifyItemRangeChanged(position, itemCount + 1);
        }
    }

    public void removeMessage(int position, ArrayList<MegaChatMessage> messages) {
        Timber.d("Size: %s", messages.size());
        this.messages = messages;
        notifyItemRemoved(position);

        if (position == messages.size() - 1) {
            Timber.d("No need to update more");
        } else {
            int itemCount = messages.size() - position;
            Timber.d("Update until end - itemCount: %s", itemCount);
            notifyItemRangeChanged(position, itemCount);
        }
    }

    private void threeDotsClicked(int currentPosition, MegaChatMessage m) {
        Timber.d("file_list_three_dots: %s", currentPosition);
        ((NodeAttachmentHistoryActivity) context).showNodeAttachmentBottomSheet(m, currentPosition);
    }

    @Override
    public boolean onLongClick(View view) {
        Timber.d("OnLongCLick");
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        ((NodeAttachmentHistoryActivity) context).activateActionMode();
        ((NodeAttachmentHistoryActivity) context).itemClick(currentPosition);

        return true;
    }

    public MegaChatMessage getMessageAt(int position) {
        try {
            if (messages != null) {
                return messages.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        Timber.d("multipleSelect: %s", multipleSelect);
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }
}