package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.listeners.SessionTransferURLListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.middlelayer.iab.MegaSku;
import mega.privacy.android.app.service.iab.BillingManagerImpl;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.billing.PaymentUtils.*;

public class UpgradeAccountFragmentLollipop extends BaseFragment implements OnClickListener{

	protected MyAccountInfo myAccountInfo;

	protected ScrollView scrollView;
	private RelativeLayout semitransparentLayer;
	private TextView textMyAccount;

	private int parameterType = -1;
	private int paymentMethod = -1;

	//PRO LITE elements:
	protected RelativeLayout proLiteLayout;
	protected TextView monthSectionLite;
	protected TextView storageSectionLite;
	protected TextView bandwidthSectionLite;
	private RelativeLayout proLiteTransparentLayout;

	//PRO I elements:
	protected RelativeLayout pro1Layout;
	protected TextView monthSectionPro1;
	protected TextView storageSectionPro1;
	protected TextView bandwidthSectionPro1;
	private RelativeLayout pro1TransparentLayout;

	//PRO II elements:
	protected RelativeLayout pro2Layout;
	protected TextView monthSectionPro2;
	protected TextView storageSectionPro2;
	protected TextView bandwidthSectionPro2;
	private RelativeLayout pro2TransparentLayout;

	//PRO III elements:
	protected RelativeLayout pro3Layout;
	protected TextView monthSectionPro3;
	protected TextView storageSectionPro3;
	protected TextView bandwidthSectionPro3;
	private RelativeLayout pro3TransparentLayout;

	//Business elements
	protected RelativeLayout businessLayout;
	protected TextView monthSectionBusiness;
	protected TextView storageSectionBusiness;
	protected TextView bandwidthSectionBusiness;

	private TextView labelCustomPlan;

	//Payment layout
	private View selectPaymentMethodLayoutLite;
	private View selectPaymentMethodLayoutPro1;
	private View selectPaymentMethodLayoutPro2;
	private View selectPaymentMethodLayoutPro3;
	private TextView selectPaymentMethod;

	private RelativeLayout googlePlayLayout;
	private RelativeLayout creditCardLayout;
	private RelativeLayout fortumoLayout;
	private RelativeLayout centiliLayout;

	private RelativeLayout googlePlayLayer;
	private RelativeLayout creditCardLayer;
	private RelativeLayout fortumoLayer;
	private RelativeLayout centiliLayer;

	private LinearLayout optionsBilling;
	private RadioGroup billingPeriod;
	private RadioButton billedMonthly;
	private RadioButton billedYearly;
	private LinearLayout layoutButtons;
	private TextView buttonContinue;

	private final static int TYPE_STORAGE_LABEL = 0;
	private final static int TYPE_TRANSFER_LABEL = 1;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logDebug("onCreateView");

		myAccountInfo = app.getMyAccountInfo();

		View v = inflater.inflate(R.layout.fragment_upgrade_account, container, false);
		scrollView = v.findViewById(R.id.scroll_view_upgrade);

		new ListenScrollChangesHelper().addViewToListen(scrollView, (v1, scrollX, scrollY, oldScrollX, oldScrollY) -> {
			if (scrollView.canScrollVertically(-1)) {
				((ManagerActivityLollipop) context).changeActionBarElevation(true);
			} else {
				((ManagerActivityLollipop) context).changeActionBarElevation(false);
			}
		});

		textMyAccount = v.findViewById(R.id.text_of_my_account);
		semitransparentLayer = v.findViewById(R.id.semitransparent_layer);
		semitransparentLayer.setOnClickListener(this);
		setAccountDetails();

		//PRO LITE ACCOUNT
		proLiteLayout = v.findViewById(R.id.upgrade_prolite_layout);
		proLiteLayout.setOnClickListener(this);
		monthSectionLite = v.findViewById(R.id.month_lite);
		storageSectionLite = v.findViewById(R.id.storage_lite);
		bandwidthSectionLite = v.findViewById(R.id.bandwidth_lite);
		selectPaymentMethodLayoutLite = v.findViewById(R.id.available_payment_methods_prolite);
		proLiteTransparentLayout = v.findViewById(R.id.upgrade_prolite_layout_transparent);
		proLiteTransparentLayout.setVisibility(View.GONE);
		//END -- PRO LITE ACCOUNT

		//PRO I ACCOUNT
		pro1Layout = v.findViewById(R.id.upgrade_pro_i_layout);
		pro1Layout.setOnClickListener(this);
		monthSectionPro1 = v.findViewById(R.id.month_pro_i);
		storageSectionPro1 = v.findViewById(R.id.storage_pro_i);
		bandwidthSectionPro1 = v.findViewById(R.id.bandwidth_pro_i);
		selectPaymentMethodLayoutPro1 = v.findViewById(R.id.available_payment_methods_pro_i);
		pro1TransparentLayout = v.findViewById(R.id.upgrade_pro_i_layout_transparent);
		pro1TransparentLayout.setVisibility(View.GONE);
		//END -- PRO I ACCOUNT

		//PRO II ACCOUNT
		pro2Layout = v.findViewById(R.id.upgrade_pro_ii_layout);
		pro2Layout.setOnClickListener(this);
		monthSectionPro2 = v.findViewById(R.id.month_pro_ii);
		storageSectionPro2 = v.findViewById(R.id.storage_pro_ii);
		bandwidthSectionPro2 = v.findViewById(R.id.bandwidth_pro_ii);
		selectPaymentMethodLayoutPro2 = v.findViewById(R.id.available_payment_methods_pro_ii);
		pro2TransparentLayout = v.findViewById(R.id.upgrade_pro_ii_layout_transparent);
		pro2TransparentLayout.setVisibility(View.GONE);
		//END -- PRO II ACCOUNT

		//PRO III ACCOUNT
		pro3Layout = v.findViewById(R.id.upgrade_pro_iii_layout);
		pro3Layout.setOnClickListener(this);
		monthSectionPro3 = v.findViewById(R.id.month_pro_iii);
		storageSectionPro3 = v.findViewById(R.id.storage_pro_iii);
		bandwidthSectionPro3 = v.findViewById(R.id.bandwidth_pro_iii);
		selectPaymentMethodLayoutPro3 = v.findViewById(R.id.available_payment_methods_pro_iii);
		pro3TransparentLayout = v.findViewById(R.id.upgrade_pro_iii_layout_transparent);
		pro3TransparentLayout.setVisibility(View.GONE);
		labelCustomPlan = v.findViewById(R.id.lbl_custom_plan);
		labelCustomPlan.setVisibility(View.GONE);
		labelCustomPlan.setOnClickListener(this);
		String strColor = getHexValue(ColorUtils.getThemeColor(context, R.attr.colorSecondary));
		String textToShowB = getString(R.string.label_custom_plan);
		textToShowB = textToShowB.replace("[A]", "<font color='" + strColor + "'>");
		textToShowB = textToShowB.replace("[/A]", "</font>");
		Spanned resultB = HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY);
		labelCustomPlan.setText(resultB);
		//END -- PRO III ACCOUNT

		//BUSINESS ACCOUNT
		businessLayout = v.findViewById(R.id.upgrade_business_layout);
		businessLayout.setOnClickListener(this);
		monthSectionBusiness = v.findViewById(R.id.month_business);
		storageSectionBusiness = v.findViewById(R.id.storage_business);
		bandwidthSectionBusiness = v.findViewById(R.id.bandwidth_business);
		//END -- BUSINESS ACCOUNT

		refreshAccountInfo();

		setPricingInfo();
		showAvailableAccount();

		return v;
	}

	public void refreshAccountInfo(){
		logDebug("Check the last call to callToPricing");
		if(callToPricing()){
			logDebug("megaApi.getPricing SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
		}

		logDebug("Check the last call to callToPaymentMethods");
		if(callToPaymentMethods()){
			logDebug("megaApi.getPaymentMethods SEND");
			((MegaApplication) ((Activity)context).getApplication()).askForPaymentMethods();
		}
	}

	/**
	 * Sets pricing info of PRO and Business plans.
	 */
	public void setPricingInfo() {
		if (myAccountInfo == null) {
			logWarning("MyAccountInfo is Null");
			return;
		}

		ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

		if (productAccounts == null) {
			logDebug("productAccounts == null");
			app.askForPricing();
			return;
		}

		for (int i = 0; i < productAccounts.size(); i++) {
			Product account = productAccounts.get(i);

			if (account.getMonths() == 1) {
				Spanned textToShow = getPriceString(account, true);
				Spanned textStorage = generateByteString(account.getStorage(), TYPE_STORAGE_LABEL);
				Spanned textTransfer = generateByteString(account.getTransfer(), TYPE_TRANSFER_LABEL);

				switch (account.getLevel()) {
					case PRO_I:
						monthSectionPro1.setText(textToShow);
						storageSectionPro1.setText(textStorage);
						bandwidthSectionPro1.setText(textTransfer);
						break;

					case PRO_II:
						monthSectionPro2.setText(textToShow);
						storageSectionPro2.setText(textStorage);
						bandwidthSectionPro2.setText(textTransfer);
						break;

					case PRO_III:
						monthSectionPro3.setText(textToShow);
						storageSectionPro3.setText(textStorage);
						bandwidthSectionPro3.setText(textTransfer);
						break;

					case PRO_LITE:
						monthSectionLite.setText(textToShow);
						storageSectionLite.setText(textStorage);
						bandwidthSectionLite.setText(textTransfer);
						break;

					case BUSINESS:
						// The initial amount of storage space for business account is 15TB
						String businessStorageSpace = getString(R.string.storage_space_amount, getSizeStringGBBased(BUSINESS_ACCOUNT_STORAGE_SPACE_AMOUNT));
						String businessTransferQuota = getString(R.string.unlimited_transfer_quota);

						try {
							businessStorageSpace = businessStorageSpace.replace("[A]", "<font color='" + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100) + "'>");
							businessStorageSpace = businessStorageSpace.replace("[/A]", "</font>");
							businessTransferQuota = businessTransferQuota.replace("[A]", "<font color='" + ColorUtils.getColorHexString(context, R.color.grey_900_grey_100) + "'>");
							businessTransferQuota = businessTransferQuota.replace("[/A]", "</font>");
						} catch (Exception e) {
							logError("Exception formatting string", e);
						}

						monthSectionBusiness.setText(textToShow);
						storageSectionBusiness.setText(HtmlCompat.fromHtml(businessStorageSpace, HtmlCompat.FROM_HTML_MODE_LEGACY));
						bandwidthSectionBusiness.setText(HtmlCompat.fromHtml(businessTransferQuota, HtmlCompat.FROM_HTML_MODE_LEGACY));
						break;
				}
			}
		}

		if (context instanceof ManagerActivityLollipop) {
			int displayedAccountType = ((ManagerActivityLollipop) context).getDisplayedAccountType();
			if (displayedAccountType != -1) {
				onUpgradeClick(displayedAccountType);
			}
		}
	}

	/**
	 * Gets a String with the price of the product. If available return the localized price price
	 * provided by Google Play or the provided by MEGA as default otherwise.
	 * @param product Product to get the corresponding price.
	 * @param monthlyBasePrice True to get a monthly base price string (i.e. "4,99 €/month") or false to get a single price (i.e. "4,99 €").
	 * @return The price of the product provided as parameter.
	 */
	private Spanned getPriceString(Product product, boolean monthlyBasePrice) {
		// First get the "default" pricing details from the MEGA server
		double price = product.getAmount() / 100.00;
		String currency = product.getCurrency();

		// Try get the local pricing details from the store if available
		MegaSku details = getSkuDetails(myAccountInfo.getAvailableSkus(), getSku(product));
		if (details != null) {
			price = details.getPriceAmountMicros() / 1000000.00;
			currency = details.getPriceCurrencyCode();
		}

		NumberFormat format = NumberFormat.getCurrencyInstance();
		format.setCurrency(Currency.getInstance(currency));
		String stringPrice = format.format(price);

		String color = ColorUtils.getColorHexString(context,R.color.grey_900_grey_100);
		if (monthlyBasePrice) {
			if (product.getMonths() != 1) {
				return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY);
			}

			switch (product.getLevel()) {
				case PRO_I:
				case PRO_II:
				case PRO_III:
					color = String.valueOf(ContextCompat.getColor(context,R.color.red_600_red_300));
					break;
				case PRO_LITE:
					color = String.valueOf(ContextCompat.getColor(context,R.color.orange_400_orange_300));
					break;
				case BUSINESS:
					color = String.valueOf(ContextCompat.getColor(context,R.color.dark_blue_500_200));
					break;
			}

			stringPrice = getString(product.getLevel() == BUSINESS ?
					R.string.type_business_month : R.string.type_month, stringPrice);
		} else {
			stringPrice = getString(product.getMonths() == 12 ?
					R.string.billed_yearly_text : R.string.billed_monthly_text, stringPrice);
		}

		try {
			stringPrice = stringPrice.replace("[A]", "<font color='" + color + "'>");
			stringPrice = stringPrice.replace("[/A]", "</font>");
		} catch (Exception e) {
			logError("Exception formatting string", e);
		}

		return HtmlCompat.fromHtml(stringPrice, HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	public void showAvailableAccount() {
		logDebug("Account type: " + myAccountInfo.getAccountType());
		switch (myAccountInfo.getAccountType()) {
			case PRO_I:
				hideProI();
				break;
			case PRO_II:
				hideProII();
				break;
			case PRO_III:
				hideProIII();
				break;
			case PRO_LITE:
				hideProLite();
				break;
		}
	}

	private void onUpgradeClick(int account){
		logDebug("account: " + account);
		RelativeLayout selectPaymentMethodClicked;

		switch (account) {
			case PRO_LITE:
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutLite;
				break;
			case PRO_I:
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro1;
				break;
			case PRO_II:
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro2;
				break;
			case PRO_III:
				selectPaymentMethodClicked = (RelativeLayout) selectPaymentMethodLayoutPro3;
				break;
			case BUSINESS:
				megaApi.getSessionTransferURL(REGISTER_BUSINESS_ACCOUNT, new SessionTransferURLListener(context));
				return;
			default:
				return;
		}

		if (myAccountInfo.getPaymentBitSet() != null){
			logDebug("myAccountInfo.getPaymentBitSet() != null");

			selectPaymentMethod = selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_method);
			TextView paymentTitle = selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_title);

			switch (account){
				case PRO_LITE:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.lite_account));
					paymentTitle.setText(getString(R.string.prolite_account));
					break;
				}
				case PRO_I:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
					paymentTitle.setText(getString(R.string.pro1_account));
					break;
				}
				case PRO_II:{
					paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
					paymentTitle.setText(getString(R.string.pro2_account));
					break;
				}
				case PRO_III: {
                    paymentTitle.setTextColor(ContextCompat.getColor(context, R.color.red_600_red_300));
                    paymentTitle.setText(getString(R.string.pro3_account));
                    break;
                }
				default:
					break;
			}

			googlePlayLayout = selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet);
			googlePlayLayout.setOnClickListener(this);

			googlePlayLayer = selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_layer);
			googlePlayLayer.setVisibility(View.GONE);

			TextView googleWalletText = selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_text);

            String textGoogleWallet = getString(BillingManagerImpl.PAY_METHOD_RES_ID);
            try{
                textGoogleWallet = textGoogleWallet.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
						+ "\'>");
                textGoogleWallet = textGoogleWallet.replace("[/A]", "</font>");
			} catch (Exception e) {
				logError("Exception formatting string", e);
			}

            googleWalletText.setText(HtmlCompat.fromHtml(textGoogleWallet, HtmlCompat.FROM_HTML_MODE_LEGACY));
            selectPaymentMethodClicked.<ImageView>findViewById(R.id.payment_method_google_wallet_icon).setImageResource(BillingManagerImpl.PAY_METHOD_ICON_RES_ID);


			creditCardLayout = selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card);
			creditCardLayout.setOnClickListener(this);

			creditCardLayer = selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_layer);
			creditCardLayer.setVisibility(View.GONE);

			TextView creditCardText = selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_text);
			String textCreditCardText = getString(R.string.payment_method_credit_card);
			try{
				textCreditCardText = textCreditCardText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
						+ "\'>");
				textCreditCardText = textCreditCardText.replace("[/A]", "</font>");
			} catch (Exception e) {
				logError("Exception formatting string", e);
			}

			creditCardText.setText(HtmlCompat.fromHtml(textCreditCardText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			fortumoLayout = selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo);
			fortumoLayout.setOnClickListener(this);

			fortumoLayer = selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_layer);
			fortumoLayer.setVisibility(View.GONE);

			TextView fortumoText = selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_text);

			String textFortumoText = getString(R.string.payment_method_fortumo);
			try{
				textFortumoText = textFortumoText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
						+ "\'>");
				textFortumoText = textFortumoText.replace("[/A]", "</font>");
			} catch (Exception e) {
				logError("Exception formatting string", e);
			}

			fortumoText.setText(HtmlCompat.fromHtml(textFortumoText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			centiliLayout = selectPaymentMethodClicked.findViewById(R.id.payment_method_centili);
			centiliLayout.setOnClickListener(this);

			centiliLayer = selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_layer);
			centiliLayer.setVisibility(View.GONE);

			TextView centiliText = selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_text);

			String textCentiliText = getString(R.string.payment_method_centili);
			try{
				textCentiliText = textCentiliText.replace("[A]", "<font color=\'"
						+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
						+ "\'>");
				textCentiliText = textCentiliText.replace("[/A]", "</font>");
			} catch (Exception e) {
				logError("Exception formatting string", e);
			}

			centiliText.setText(HtmlCompat.fromHtml(textCentiliText, HtmlCompat.FROM_HTML_MODE_LEGACY));

			optionsBilling = selectPaymentMethodClicked.findViewById(R.id.options);

			billingPeriod = selectPaymentMethodClicked.findViewById(R.id.billing_period);
			billedMonthly = selectPaymentMethodClicked.findViewById(R.id.billed_monthly);
			billedMonthly.setOnClickListener(this);
			billedYearly = selectPaymentMethodClicked.findViewById(R.id.billed_yearly);
			billedYearly.setOnClickListener(this);

			layoutButtons = selectPaymentMethodClicked.findViewById(R.id.layout_buttons);
			TextView buttonCancel = selectPaymentMethodClicked.findViewById(R.id.button_cancel);
			buttonCancel.setOnClickListener(this);
            buttonContinue = selectPaymentMethodClicked.findViewById(R.id.button_continue);
            buttonContinue.setOnClickListener(this);

            buttonContinue.setEnabled(false);
			buttonContinue.setTextColor((ContextCompat.getColor(context, R.color.grey_700_026_grey_300_026)));

			googlePlayLayout.setVisibility(View.GONE);
			creditCardLayout.setVisibility(View.GONE);
			fortumoLayout.setVisibility(View.GONE);
			centiliLayout.setVisibility(View.GONE);
            layoutButtons.setVisibility(View.GONE);
			optionsBilling.setVisibility(View.GONE);

			showPaymentMethods(account);

			refreshAccountInfo();
			logDebug("END refreshAccountInfo");
			if (!myAccountInfo.isInventoryFinished()) {
				logDebug("if (!myAccountInfo.isInventoryFinished())");
				googlePlayLayout.setVisibility(View.GONE);
			}
			logDebug("Just before show the layout");

			selectPaymentMethodClicked.setVisibility(View.VISIBLE);
			semitransparentLayer.setVisibility(View.VISIBLE);

			switch (account) {
				case PRO_I:
					new Handler().post(() -> scrollView.smoothScrollTo(0, pro1Layout.getTop()));
					break;
				case PRO_II:
					new Handler().post(() -> scrollView.smoothScrollTo(0, pro2Layout.getTop()));
					break;
				case PRO_III:
					new Handler().post(() -> scrollView.smoothScrollTo(0, pro3Layout.getBottom()));
					break;
			}
		} else {
			logWarning("PaymentBitSet Null");
		}
	}

	private void hideProLite(){
		logDebug("hideProLite");
		proLiteTransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProI(){
		logDebug("hideProI");
		pro1TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProII(){
		logDebug("hideProII");
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}

	private void hideProIII(){
		logDebug("hideProIII");
		pro3TransparentLayout.setVisibility(View.VISIBLE);
		labelCustomPlan.setVisibility(View.VISIBLE);
	}

	private Spanned generateByteString(long gb, int labelType) {
		String textToShow = "[A] " + getSizeStringGBBased(gb) + " [/A] " + storageOrTransferLabel(labelType);

		try {
			textToShow = textToShow.replace("[A]", "<font color=\'"
					+ ColorUtils.getColorHexString(context, R.color.grey_900_grey_100)
					+ "\'>");
			textToShow = textToShow.replace("[/A]", "</font>");
		} catch (Exception e) {
			logError("Exception formatting string", e);
		}

		return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY);
	}

	private String storageOrTransferLabel(int labelType) {
		switch (labelType) {
			case TYPE_STORAGE_LABEL:
				return getString(R.string.label_storage_upgrade_account);
			case TYPE_TRANSFER_LABEL:
				return getString(R.string.label_transfer_quota_upgrade_account);
			default:
				return "";
		}
	}

	private void showNextPaymentFragment(int paymentM) {
		logDebug("paymentM: " + paymentM);

		if (selectPaymentMethodLayoutLite.getVisibility() == View.VISIBLE) {
			parameterType = PRO_LITE;
		} else if (selectPaymentMethodLayoutPro1.getVisibility() == View.VISIBLE) {
			parameterType = PRO_I;
		} else if (selectPaymentMethodLayoutPro2.getVisibility() == View.VISIBLE) {
			parameterType = PRO_II;
		} else if (selectPaymentMethodLayoutPro3.getVisibility() == View.VISIBLE) {
			parameterType = PRO_III;
		} else {
			parameterType = 0;
		}
		paymentMethod = paymentM;
		logDebug("parameterType: " + parameterType);

		((ManagerActivityLollipop) context).setSelectedAccountType(parameterType);
		((ManagerActivityLollipop) context).setSelectedPaymentMethod(paymentMethod);
		showmyF(paymentM, parameterType);
	}

	private void contactForCustomPlan() {
		logDebug("Send Feedback");
        ((ManagerActivityLollipop) context).askForCustomizedPlan();
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		switch (v.getId()){
			case R.id.lbl_custom_plan: {
                contactForCustomPlan();
				break;
			}
            case R.id.button_continue:{
				logDebug("Button button_continue pressed");
				if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
					//MONTHLY SUBSCRIPTION
					switch (parameterType) {
						case PRO_I: {
							//PRO I
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_I_MONTH);
									break;
								}
							}
							break;
						}
						case PRO_II: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_II_MONTH);
									break;
								}
							}
							break;
						}
						case PRO_III: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_III_MONTH);
									break;
								}
							}
							break;
						}
						case PRO_LITE: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_MONTH, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_LITE_MONTH);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_FORTUMO: {
									((ManagerActivityLollipop) context).showFortumo();
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_CENTILI: {
									((ManagerActivityLollipop) context).showCentili();
									break;
								}
							}
							break;
						}
					}
				} else {
					//YEARLY SUBSCRIPTION
					switch (parameterType) {
						case PRO_I: {
							//PRO I
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_I_YEAR);
									break;
								}
							}
							break;
						}
						case PRO_II: {
							//PRO II
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_II_YEAR);
									break;
								}
							}
							break;
						}
						case PRO_III: {
							//PRO III
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_III_YEAR);
									break;
								}
							}
							break;
						}
						case PRO_LITE: {
							//LITE
							switch (paymentMethod) {
								case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD: {
									((ManagerActivityLollipop) context).showCC(parameterType, PAYMENT_CC_YEAR, true);
									break;
								}
								case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET: {
									((ManagerActivityLollipop) context).launchPayment(SKU_PRO_LITE_YEAR);
									break;
								}
							}
							break;
						}
					}
				}

                break;
            }
			case R.id.button_cancel:{
				logDebug("button_cancel");
				semitransparentLayer.setVisibility(View.GONE);
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.semitransparent_layer:{
				logDebug("semitransparent_layer");
				semitransparentLayer.setVisibility(View.GONE);
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.upgrade_prolite_layout:{
				if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
				}else{
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_LITE);
				}
				break;
			}
			case R.id.upgrade_pro_i_layout:{
				if (selectPaymentMethodLayoutPro1.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_I);
				}
				break;
			}
			case R.id.upgrade_pro_ii_layout:{
				if (selectPaymentMethodLayoutPro2.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(PRO_II);
				}
				break;
			}
			case R.id.upgrade_pro_iii_layout:{
				if (selectPaymentMethodLayoutPro3.getVisibility() == View.VISIBLE) {
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				} else {
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					semitransparentLayer.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					onUpgradeClick(PRO_III);
				}
				break;
			}
			case R.id.payment_method_google_wallet:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET);
				break;
			}
			case R.id.payment_method_credit_card:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD);
				break;
			}
			case R.id.payment_method_fortumo:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_FORTUMO);
				break;
			}
			case R.id.payment_method_centili:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CENTILI);
				break;
			}
			case R.id.upgrade_business_layout:{
				megaApi.getSessionTransferURL(REGISTER_BUSINESS_ACCOUNT, new SessionTransferURLListener(context));
				break;
			}
		}
	}

	private void showPaymentMethods(int parameterType){
		logDebug("parameterType: " + parameterType);

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		switch(parameterType){
			case PRO_I:{
				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					logWarning("Not payment bit set received!!!");
					selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					googlePlayLayout.setVisibility(View.GONE);
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);
				}

				break;
			}
			case PRO_II:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					logWarning("Not payment bit set received!!!");
				}

				break;
			}
			case PRO_III:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}

				break;
			}
			case PRO_LITE:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						logDebug("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else {
                        if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
                            googlePlayLayout.setVisibility(View.VISIBLE);
                            layoutButtons.setVisibility(View.VISIBLE);
                        }
					}

					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
						fortumoLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }
					if (checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						centiliLayout.setVisibility(View.VISIBLE);
                        layoutButtons.setVisibility(View.VISIBLE);

                    }

					if(!isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				break;
			}
		}
	}

	private void setAccountDetails() {
		logDebug("setAccountDetails");

		if ((getActivity() == null) || (!isAdded())) {
			logWarning("Fragment MyAccount NOT Attached!");
			return;
		}

		//Set account details
		if (myAccountInfo.getAccountType() < FREE || myAccountInfo.getAccountType() > PRO_LITE) {
			textMyAccount.setText(getString(R.string.recovering_info));
			textMyAccount.setTextColor(ContextCompat.getColor(context,R.color.grey_054_white_054));
		} else {
			String textToShow;
			String color;
			switch (myAccountInfo.getAccountType()) {
				case FREE:
				default:
					textToShow = getString(R.string.type_of_my_account, getString(R.string.free_account));
					color = String.valueOf(ContextCompat.getColor(context, R.color.green_500_green_400));
					break;
				case PRO_I:
					textToShow = getString(R.string.type_of_my_account, getString(R.string.pro1_account));
					color = String.valueOf(ContextCompat.getColor(context, R.color.red_600_red_300));
					break;
				case PRO_II:
					textToShow = getString(R.string.type_of_my_account, getString(R.string.pro2_account));
					color = String.valueOf(ContextCompat.getColor(context, R.color.red_600_red_300));
					break;
				case PRO_III:
					textToShow = getString(R.string.type_of_my_account, getString(R.string.pro3_account));
					color = String.valueOf(ContextCompat.getColor(context, R.color.red_600_red_300));
					break;
				case PRO_LITE:
					textToShow = getString(R.string.type_of_my_account, getString(R.string.prolite_account));
					color = String.valueOf(ContextCompat.getColor(context, R.color.lite_account));
					break;
			}

			try {
				textToShow = textToShow.replace("[A]", "<font color='" + color + "'>");
				textToShow = textToShow.replace("[/A]", "</font>");
			} catch (Exception e) {
				logWarning("Exception formatting string", e);
			}

			textMyAccount.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
		}
	}

	private void showmyF(int paymentMethod, int parameterType){
		logDebug("paymentMethod " + paymentMethod + ", type " + parameterType);

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			logWarning("accounts == null");
			((MegaApplication) ((Activity)context).getApplication()).askForPricing();
			return;
		}

		for (int i = 0; i < accounts.size(); i++) {

			Product account = accounts.get(i);

			if (account.getLevel() == parameterType) {
				Spanned textToShow = getPriceString(account, false);
				if (account.getMonths() == 1) {
					billedMonthly.setText(textToShow);
				} else if (account.getMonths() == 12) {
					billedYearly.setText(textToShow);
				}
			}
		}

		switch (parameterType) {
			case PRO_I:
				logDebug("case PRO I");

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro I - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro I - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro I - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro I - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_I_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_I_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;

			case PRO_II:
				logDebug(" case PRO II");

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro II - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro II - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro II - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro II - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_II_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_II_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;

			case PRO_III:
				logDebug("case PRO III");

				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Pro III - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Pro III - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Pro III - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Pro III - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						if (myAccountInfo.isPurchasedAlready(SKU_PRO_III_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_III_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}

				break;

			case PRO_LITE:
				logDebug("case LITE");
				switch (paymentMethod){
					case MegaApiAndroid.PAYMENT_METHOD_FORTUMO:{
						logDebug("Lite - PAYMENT_METHOD_FORTUMO");
						creditCardLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						fortumoLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedMonthly.setChecked(true);
						billedYearly.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CENTILI:{
						logDebug("Lite - PAYMENT_METHOD_CENTILI");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedMonthly.setChecked(true);
						billedYearly.setVisibility(View.GONE);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD:{
						logDebug("Lite - PAYMENT_METHOD_CREDIT_CARD");
						fortumoLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);
						billedYearly.setChecked(true);
						break;
					}
					case MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET:{
						logDebug("Lite - PAYMENT_METHOD_GOOGLE_WALLET");
						fortumoLayer.setVisibility(View.VISIBLE);
						creditCardLayer.setVisibility(View.VISIBLE);
						centiliLayer.setVisibility(View.VISIBLE);
						googlePlayLayer.setVisibility(View.GONE);
						optionsBilling.setVisibility(View.VISIBLE);
						buttonContinue.setEnabled(true);
						buttonContinue.setTextColor(ColorUtils.getThemeColor(context,R.attr.colorSecondary));
						billedMonthly.setVisibility(View.VISIBLE);
						billedYearly.setVisibility(View.VISIBLE);

						if (myAccountInfo.isPurchasedAlready(SKU_PRO_LITE_MONTH)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_monthly){
								billedYearly.setChecked(true);
							}
							billedMonthly.setVisibility(View.GONE);
						}
						if (myAccountInfo.isPurchasedAlready(SKU_PRO_LITE_YEAR)) {
							if(billingPeriod.getCheckedRadioButtonId()==R.id.billed_yearly){
								billedMonthly.setChecked(true);
							}
							billedYearly.setVisibility(View.GONE);
						}
						break;
					}
				}
				break;
		}
	}
}
