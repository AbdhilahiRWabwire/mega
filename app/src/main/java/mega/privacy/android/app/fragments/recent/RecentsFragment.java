package mega.privacy.android.app.fragments.recent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.brandongogetap.stickyheaders.exposed.StickyHeader;
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.fragments.homepage.Scrollable;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RecentsAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.Util.getMediaIntent;


public class RecentsFragment extends Fragment implements StickyHeaderHandler, Scrollable {

    public static ImageView imageDrag;

    private Context context;
    private DisplayMetrics outMetrics;

    private MegaApiAndroid megaApi;

    private ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<>();
    private ArrayList<MegaRecentActionBucket> buckets;
    private MegaRecentActionBucket bucketSelected;
    private ArrayList<RecentsItem> recentsItems;
    private RecentsAdapter adapter;

    private RelativeLayout emptyLayout;
    private ImageView emptyImage;
    private TextView emptyText;

    private StickyLayoutManager stickyLayoutManager;
    private RecyclerView listView;
    private FastScroller fastScroller;

    private SelectedBucketViewModel selectedBucketModel;

    private final Observer<Boolean> nodeChangeObserver = forceUpdate -> {
        if (forceUpdate) {
            buckets = megaApi.getRecentActions();
            reloadItems(buckets);
            refreshRecentsActions();
            setVisibleContacts();
            setRecentsView();
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        ((ManagerActivityLollipop) requireActivity()).pagerRecentsFragmentOpened(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        ((ManagerActivityLollipop) requireActivity()).pagerRecentsFragmentClosed(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        imageDrag = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
        if (megaApi.getRootNode() == null) return null;

        recentsItems = new ArrayList<>();

        buckets = megaApi.getRecentActions();

        View v = inflater.inflate(R.layout.fragment_recents, container, false);

        emptyLayout = v.findViewById(R.id.empty_state_recents);

        emptyImage = v.findViewById(R.id.empty_image_recents);

        emptyText = v.findViewById(R.id.empty_text_recents);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImage.setImageResource(R.drawable.empty_recents_landscape);
        } else {
            emptyImage.setImageResource(R.drawable.empty_recents_portrait);
        }

        String textToShow = StringResourcesUtils.getString(R.string.context_empty_recents);

        try {
            textToShow = textToShow.replace("[A]","<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
                    + "\'>");
            textToShow = textToShow.replace("[/A]","</font>");
            textToShow = textToShow.replace("[B]","<font color=\'"
                    + ColorUtils.getColorHexString(context, R.color.grey_300_grey_600)
                    + "\'>");
            textToShow = textToShow.replace("[/B]","</font>");
        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyText.setText(result);

        listView = v.findViewById(R.id.list_view_recents);
        fastScroller = v.findViewById(R.id.fastscroll);

        stickyLayoutManager = new TopSnappedStickyLayoutManager(context, this);
        listView.setLayoutManager(stickyLayoutManager);
        listView.setClipToPadding(false);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
        fillRecentItems(buckets);
        setRecentsView();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean.class).observeForever(nodeChangeObserver);
        selectedBucketModel = new ViewModelProvider(requireActivity()).get(SelectedBucketViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean.class).removeObserver(nodeChangeObserver);
    }

    public SelectedBucketViewModel getSelectedBucketModel() {
        return selectedBucketModel;
    }

    public void fillRecentItems(ArrayList<MegaRecentActionBucket> buckets) {
        this.buckets = buckets;
        reloadItems(buckets);

        adapter = new RecentsAdapter(context, this, recentsItems);
        listView.setAdapter(adapter);
        listView.addItemDecoration(new HeaderItemDecoration(context));
        setVisibleContacts();
    }

    private void reloadItems(ArrayList<MegaRecentActionBucket> buckets) {
        recentsItems.clear();
        String previousDate = "";
        String currentDate;

        for (int i = 0; i < buckets.size(); i++) {
            RecentsItem item = new RecentsItem(context, buckets.get(i));
            if (i == 0) {
                previousDate = currentDate = item.getDate();
                recentsItems.add(new RecentsItemHeader(currentDate));
            } else {
                currentDate = item.getDate();
                if (!currentDate.equals(previousDate)) {
                    recentsItems.add(new RecentsItemHeader(currentDate));
                    previousDate = currentDate;
                }
            }
            recentsItems.add(item);
        }
    }

    public void refreshRecentsActions() {
        if (adapter != null) {
            adapter.setItems(recentsItems);
        }
        setRecentsView();
    }

    private void setRecentsView() {
        if (buckets == null || buckets.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            fastScroller.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            fastScroller.setRecyclerView(listView);
            if (buckets.size() < MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
        ((ManagerActivityLollipop) context).setToolbarTitle();
        checkScroll();
    }

    @Override
    public void checkScroll() {
        if (listView == null) return;
        LiveEventBus.get(EVENT_SCROLLING_CHANGE, Pair.class)
                .post(new Pair<>(this, listView.canScrollVertically(-1)
                        && listView.getVisibility() == View.VISIBLE));
    }

    public String findUserName(String mail) {
        for (MegaContactAdapter contact : visibleContacts) {
            if (contact.getMegaUser().getEmail().equals(mail)) {
                return contact.getFullName();
            }
        }
        return "";
    }

    public void setVisibleContacts() {
        visibleContacts.clear();
        ArrayList<MegaUser> contacts = megaApi.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                long contactHandle = contacts.get(i).getHandle();
                MegaContactDB contactDB = getContactDB(contactHandle);
                if (contactDB == null) break;
                String fullName = getContactNameDB(contactDB);
                if (fullName == null) {
                    fullName = contacts.get(i).getEmail();
                }
                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
                visibleContacts.add(megaContactAdapter);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private long[] getBucketNodeHandles(boolean areImages) {
        if (bucketSelected == null || bucketSelected.getNodes() == null || bucketSelected.getNodes().size() == 0)
            return null;

        MegaNode node;
        MegaNodeList list = bucketSelected.getNodes();
        ArrayList<Long> nodeHandlesList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            node = list.get(i);
            if (node == null) continue;

//          Group handles by type of file
            if (areImages && MimeTypeList.typeForName(node.getName()).isImage()) {
//              only images on the one hand
                nodeHandlesList.add(node.getHandle());
            } else if (!areImages && isAudioOrVideo(node) && isInternalIntent(node)) {
//              only videos or audios on the other
                nodeHandlesList.add(node.getHandle());
            }
        }

        long[] nodeHandles = new long[nodeHandlesList.size()];
        for (int i = 0; i < nodeHandlesList.size(); i++) {
            nodeHandles[i] = nodeHandlesList.get(i);
        }

        return nodeHandles;
    }

    public ImageView getImageDrag(long handle) {
        return adapter.getThumbnailView(listView, handle);
    }

    public void openFile(MegaNode node, boolean isMedia, ImageView thumbnail) {
        imageDrag = thumbnail;

        int[] screenPosition = null;
        if (thumbnail != null) {
            screenPosition = new int[4];

            int[] loc = new int[2];
            thumbnail.getLocationOnScreen(loc);

            screenPosition[0] = loc[0];
            screenPosition[1] = loc[1];
            screenPosition[2] = thumbnail.getWidth();
            screenPosition[3] = thumbnail.getHeight();
        }

        Intent intent = null;

        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            intent = new Intent(context, FullScreenImageViewerLollipop.class);
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_ADAPTER);
            if (screenPosition != null) {
                intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition);
            }
            intent.putExtra(HANDLE, node.getHandle());
            if (isMedia) {
                intent.putExtra(NODE_HANDLES, getBucketNodeHandles(true));
            }

            context.startActivity(intent);
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            return;
        }

        String localPath = getLocalFile(context, node.getName(), node.getSize());
        boolean paramsSetSuccessfully = false;

        if (isAudioOrVideo(node)) {
            if (isInternalIntent(node)) {
                intent = getMediaIntent(context, node.getName());
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
            }

            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_ADAPTER);
            if (screenPosition != null) {
                intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition);
                int[] screenPositionForSwipeDismiss = new int[]{
                        screenPosition[0] + screenPosition[2] / 2,
                        screenPosition[1] + screenPosition[3] / 2,
                        screenPosition[2],
                        screenPosition[3]
                };
                intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION_FOR_SWIPE_DISMISS, screenPositionForSwipeDismiss);

            }
            intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());
            if (isMedia) {
                intent.putExtra(NODE_HANDLES, getBucketNodeHandles(false));
                intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true);
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);
            }

            if (isLocalFile(node, megaApi, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(context, node, intent, localPath, false);
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(context, node, megaApi, intent);
            }

            if (paramsSetSuccessfully) {
                intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle());
                if (isOpusFile(node)) {
                    intent.setDataAndType(intent.getData(), "audio/*");
                }
            }
        } else if (MimeTypeList.typeForName(node.getName()).isURL()) {
            intent = new Intent(Intent.ACTION_VIEW);

            if (isLocalFile(node, megaApi, localPath)) {
                paramsSetSuccessfully = setURLIntentParams(context, node, intent, localPath);
            }
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            intent = new Intent(context, PdfViewerActivityLollipop.class);
            intent.putExtra(INTENT_EXTRA_KEY_INSIDE, true);
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_ADAPTER);
            if (screenPosition != null) {
                intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition);
            }

            if (isLocalFile(node, megaApi, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(context, node, intent, localPath, false);
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(context, node, megaApi, intent);
            }

            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle());
        }

        if (intent != null && !isIntentAvailable(context, intent)) {
            paramsSetSuccessfully = false;
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
        }

        if (paramsSetSuccessfully) {
            context.startActivity(intent);
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            return;
        }

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(node.getHandle());
        NodeController nC = new NodeController(context);
        nC.prepareForDownload(handleList, true);
    }

    public void setBucketSelected(MegaRecentActionBucket bucketSelected) {
        this.bucketSelected = bucketSelected;
    }

    @Override
    public List<RecentsItem> getAdapterData() {
        return recentsItems;
    }

    public class RecentsItemHeader extends RecentsItem implements StickyHeader {

        public RecentsItemHeader(String date) {
            super(date);
        }
    }
}
