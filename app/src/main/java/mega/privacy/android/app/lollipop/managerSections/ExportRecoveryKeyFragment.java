package mega.privacy.android.app.lollipop.managerSections;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ExportRecoveryKeyFragment extends Fragment implements View.OnClickListener{

    private DatabaseHandler dbH;
    private MegaApiAndroid megaApi;
    private Context context;

    private RelativeLayout exportMKLayout;
    private TextView titleExportMK;
    private TextView subTitleExportMK;
    private TextView firstParExportMK;
    private TextView secondParExportMK;
    private TextView thirdParExportMK;
    private TextView actionExportMK;
    private Button printMK;
    private Button copyMK;
    private Button saveMK;

    DisplayMetrics outMetrics;

    public static ExportRecoveryKeyFragment newInstance() {
        logDebug("newInstance");
        ExportRecoveryKeyFragment fragment = new ExportRecoveryKeyFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        View v = inflater.inflate(R.layout.export_mk_layout, container, false);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        exportMKLayout = (RelativeLayout) v.findViewById(R.id.export_mk_full_layout);
        LinearLayout.LayoutParams exportMKButtonsParams = (LinearLayout.LayoutParams)exportMKLayout.getLayoutParams();
        exportMKButtonsParams.setMargins(0, 0, 0, scaleHeightPx(10, outMetrics));
        exportMKLayout.setLayoutParams(exportMKButtonsParams);

        titleExportMK = (TextView) v.findViewById(R.id.title_export_MK_layout);
        RelativeLayout.LayoutParams titleExportMKParams = (RelativeLayout.LayoutParams)titleExportMK.getLayoutParams();
        titleExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(50, outMetrics), px2dp(24, outMetrics), 0);
        titleExportMK.setLayoutParams(titleExportMKParams);

        subTitleExportMK = (TextView) v.findViewById(R.id.subtitle_export_MK_layout);
        RelativeLayout.LayoutParams subTitleExportMKParams = (RelativeLayout.LayoutParams)subTitleExportMK.getLayoutParams();
        subTitleExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(24, outMetrics), px2dp(24, outMetrics), 0);
        subTitleExportMK.setLayoutParams(subTitleExportMKParams);

        firstParExportMK = (TextView) v.findViewById(R.id.first_par_export_MK_layout);
        RelativeLayout.LayoutParams firstParExportMKParams = (RelativeLayout.LayoutParams)firstParExportMK.getLayoutParams();
        firstParExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(20, outMetrics), px2dp(24, outMetrics), 0);
        firstParExportMK.setLayoutParams(firstParExportMKParams);

        secondParExportMK = (TextView) v.findViewById(R.id.second_par_export_MK_layout);
        RelativeLayout.LayoutParams secondParExportMKParams = (RelativeLayout.LayoutParams)secondParExportMK.getLayoutParams();
        secondParExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(20, outMetrics), px2dp(24, outMetrics), 0);
        secondParExportMK.setLayoutParams(secondParExportMKParams);

        thirdParExportMK = (TextView) v.findViewById(R.id.third_par_export_MK_layout);
        RelativeLayout.LayoutParams thirdParExportMKParams = (RelativeLayout.LayoutParams)thirdParExportMK.getLayoutParams();
        thirdParExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(24, outMetrics), px2dp(24, outMetrics), 0);
        thirdParExportMK.setLayoutParams(thirdParExportMKParams);

        actionExportMK = (TextView) v.findViewById(R.id.action_export_MK_layout);
        RelativeLayout.LayoutParams actionExportMKParams = (RelativeLayout.LayoutParams)actionExportMK.getLayoutParams();
        actionExportMKParams.setMargins(px2dp(24, outMetrics), scaleHeightPx(20, outMetrics), px2dp(24, outMetrics), 0);
        actionExportMK.setLayoutParams(actionExportMKParams);

        printMK = v.findViewById(R.id.print_MK_button);
        printMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        printMK.setOnClickListener(this);

        copyMK = (Button) v.findViewById(R.id.copy_MK_button);
        copyMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));

        copyMK.setOnClickListener(this);

        saveMK = (Button) v.findViewById(R.id.save_MK_button);
        saveMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        saveMK.setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        logDebug("onAttach");
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public void onAttach(Context context) {
        logDebug("onAttach context");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.print_MK_button:{
                hideMKLayout();
                AccountController aC = new AccountController(context);
                aC.printRK();
                break;
            }
            case R.id.copy_MK_button:{
                hideMKLayout();
                AccountController aC = new AccountController(context);
                aC.copyMK(false);
                break;
            }
            case R.id.save_MK_button:{
                if (checkStoragePermission()) {
                    toFileSystem();
                }
                break;
            }
        }
    }

    private void toFileSystem() {
        hideMKLayout();
        AccountController aC = new AccountController(context);
        aC.saveRkToFileSystem();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toFileSystem();
            } else {
                logWarning("Don't grant the write storage permission when export RK.");
            }
        }
    }

    private void hideMKLayout() {
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideMKLayout();
        }
    }
}
