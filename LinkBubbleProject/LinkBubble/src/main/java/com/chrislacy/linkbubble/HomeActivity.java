package com.chrislacy.linkbubble;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.chrislacy.linkbubble.R;
import com.flavienlaurent.notboringactionbar.AlphaForegroundColorSpan;
import com.flavienlaurent.notboringactionbar.KenBurnsView;
import com.google.android.apps.dashclock.ui.SwipeDismissListViewTouchListener;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class HomeActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "HomeActivity";
    private int mActionBarTitleColor;
    private int mActionBarHeight;
    private int mHeaderHeight;
    private int mMinHeaderTranslation;
    private ListView mListView;
    private LinkHistoryAdapter mHistoryAdapter;
    private List<LinkHistoryRecord> mLinkHistoryRecords;
    private KenBurnsView mHeaderPicture;
    private ImageView mHeaderLogo;
    private View mHeader;
    private View mPlaceHolderView;
    private AccelerateDecelerateInterpolator mSmoothInterpolator;

    private RectF mRect1 = new RectF();
    private RectF mRect2 = new RectF();

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mSpannableString;

    private TypedValue mTypedValue = new TypedValue();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSmoothInterpolator = new AccelerateDecelerateInterpolator();
        mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.header_height);
        mMinHeaderTranslation = -mHeaderHeight + getActionBarHeight();

        setContentView(R.layout.activity_home);

        mListView = (ListView) findViewById(R.id.listview);
        mHeader = findViewById(R.id.header);
        mHeaderPicture = (KenBurnsView) findViewById(R.id.header_picture);
        mHeaderPicture.setResourceIds(R.drawable.picture0, R.drawable.picture1);
        mHeaderLogo = (ImageView) findViewById(R.id.header_logo);

        mActionBarTitleColor = getResources().getColor(R.color.actionbar_title_color);

        mSpannableString = new SpannableString(getString(R.string.home_activity_label));
        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(mActionBarTitleColor);

        setupActionBar();

        mPlaceHolderView = getLayoutInflater().inflate(R.layout.view_home_header, mListView, false);
        mListView.addHeaderView(mPlaceHolderView);
    }

    @Override
    public void onStart() {
        super.onStart();

        updateListViewData();

        ((MainApplication)getApplicationContext()).getBus().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        ((MainApplication)getApplicationContext()).getBus().unregister(this);
    }

    private void setupListView() {
        MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
        mLinkHistoryRecords = databaseHelper.getAllLinkHistoryRecords();
        if (mLinkHistoryRecords == null || mLinkHistoryRecords.size() == 0) {
            return;
        }

        mHistoryAdapter = new LinkHistoryAdapter(this);

        mListView.setAdapter(mHistoryAdapter);

        final SwipeDismissListViewTouchListener swipeDismissTouchListener =
                new SwipeDismissListViewTouchListener(
                        mListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            public boolean canDismiss(int position) {
                                //return position < mSelectedExtensionsAdapter.getCount() - 1;
                                return true;
                            }

                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                /*
                                for (int position : reverseSortedPositions) {
                                    mSelectedExtensions.remove(position);
                                }
                                repopulateAvailableExtensions();
                                */
                                mHistoryAdapter.notifyDataSetChanged();
                            }
                        });
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                swipeDismissTouchListener.setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int scrollY = getScrollY();
                //sticky actionbar
                mHeader.setTranslationY(Math.max(-scrollY, mMinHeaderTranslation));
                //header_logo --> actionbar icon
                float ratio = clamp(mHeader.getTranslationY() / mMinHeaderTranslation, 0.0f, 1.0f);
                interpolate(mHeaderLogo, getActionBarIconView(), mSmoothInterpolator.getInterpolation(ratio));
                //actionbar title alpha
                //getActionBarTitleView().setAlpha(clamp(5.0F * ratio - 4.0F, 0.0F, 1.0F));
                //---------------------------------
                //better way thanks to @cyrilmottier
                setTitleAlpha(clamp(5.0F * ratio - 4.0F, 0.0F, 1.0F));
            }
        });

        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return swipeDismissTouchListener.onTouch(view, motionEvent);
            }
        });
    }

    void updateListViewData() {
        if (mLinkHistoryRecords != null) {
            synchronized (mLinkHistoryRecords) {
                setupListView();
            }
        } else {
            setupListView();
        }
    }

    private void setTitleAlpha(float alpha) {
        mAlphaForegroundColorSpan.setAlpha(alpha);
        mSpannableString.setSpan(mAlphaForegroundColorSpan, 0, mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(mSpannableString);
    }

    public static float clamp(float value, float max, float min) {
        return Math.max(Math.min(value, min), max);
    }

    private void interpolate(View view1, View view2, float interpolation) {
        getOnScreenRect(mRect1, view1);
        getOnScreenRect(mRect2, view2);

        float scaleX = 1.0F + interpolation * (mRect2.width() / mRect1.width() - 1.0F);
        float scaleY = 1.0F + interpolation * (mRect2.height() / mRect1.height() - 1.0F);
        float translationX = 0.5F * (interpolation * (mRect2.left + mRect2.right - mRect1.left - mRect1.right));
        float translationY = 0.5F * (interpolation * (mRect2.top + mRect2.bottom - mRect1.top - mRect1.bottom));

        view1.setTranslationX(translationX);
        view1.setTranslationY(translationY - mHeader.getTranslationY());
        view1.setScaleX(scaleX);
        view1.setScaleY(scaleY);
    }

    private RectF getOnScreenRect(RectF rect, View view) {
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        return rect;
    }

    public int getScrollY() {
        View c = mListView.getChildAt(0);
        if (c == null) {
            return 0;
        }

        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int top = c.getTop();

        int headerHeight = 0;
        if (firstVisiblePosition >= 1) {
            headerHeight = mPlaceHolderView.getHeight();
        }

        return -top + firstVisiblePosition * c.getHeight() + headerHeight;
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setIcon(R.drawable.ic_transparent);
        actionBar.setDisplayHomeAsUpEnabled(false);

        //getActionBarTitleView().setAlpha(0f);
    }

    private ImageView getActionBarIconView() {
        return (ImageView) findViewById(android.R.id.home);
    }

    /*private TextView getActionBarTitleView() {
        int id = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        return (TextView) findViewById(id);
    }*/

    public int getActionBarHeight() {
        if (mActionBarHeight != 0) {
            return mActionBarHeight;
        }
        getTheme().resolveAttribute(android.R.attr.actionBarSize, mTypedValue, true);
        mActionBarHeight = TypedValue.complexToDimensionPixelSize(mTypedValue.data, getResources().getDisplayMetrics());
        return mActionBarHeight;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_activity, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            }

            case R.id.action_upgrade_to_pro: {
                Intent intent = Config.getStoreIntent(this, Config.STORE_PRO_URL);
                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
            }

            case R.id.action_clear_history: {
                MainDatabaseHelper databaseHelper = ((MainApplication)getApplication()).mDatabaseHelper;
                databaseHelper.deleteAllLinkHistoryRecords();
                mLinkHistoryRecords = null;
                mHistoryAdapter.notifyDataSetChanged();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof LinkHistoryRecord) {
            LinkHistoryRecord linkHistoryRecord = (LinkHistoryRecord)view.getTag();
            MainApplication.openLink(this, linkHistoryRecord.getUrl(), true);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() instanceof LinkHistoryRecord) {
            final LinkHistoryRecord linkHistoryRecord = (LinkHistoryRecord)view.getTag();
            Resources resources = getResources();
            
            PopupMenu popupMenu;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                popupMenu = new PopupMenu(this, view, Gravity.RIGHT);
            } else {
                popupMenu = new PopupMenu(this, view);
            }

            String defaultBrowserLabel = Settings.get().getDefaultBrowserLabel();
            if (defaultBrowserLabel != null) {
                popupMenu.getMenu().add(Menu.NONE, R.id.item_open_in_browser, Menu.NONE,
                        String.format(resources.getString(R.string.action_open_in_browser), defaultBrowserLabel));
            }

            popupMenu.getMenu().add(Menu.NONE, R.id.item_share, Menu.NONE,
                    resources.getString(R.string.action_share));

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_open_in_browser: {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(linkHistoryRecord.getUrl()));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            MainApplication.loadInBrowser(HomeActivity.this, intent, true);
                            return true;
                        }

                        case R.id.item_share: {
                            AlertDialog alertDialog = ActionItem.getShareAlert(HomeActivity.this, new ActionItem.OnActionItemSelectedListener() {
                                @Override
                                public void onSelected(ActionItem actionItem) {
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    intent.setClassName(actionItem.mPackageName, actionItem.mActivityClassName);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(Intent.EXTRA_TEXT, linkHistoryRecord.getUrl());
                                    startActivity(intent);
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    return false;
                }
            });
            popupMenu.show();
            return true;
        }

        return false;
    }


    private class LinkHistoryAdapter extends BaseAdapter {

        Context mContext;
        Date mDate;

        public LinkHistoryAdapter(Context context) {
            mContext = context;
            mDate = new Date();
        }

        @Override
        public int getCount() {
            return mLinkHistoryRecords != null ? mLinkHistoryRecords.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mLinkHistoryRecords != null ? mLinkHistoryRecords.get(position) : position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.history_item, parent, false);
            }

            LinkHistoryRecord linkHistoryRecord = mLinkHistoryRecords.get(position);

            TextView title = (TextView) convertView.findViewById(R.id.page_title);
            title.setText(linkHistoryRecord.getTitle());

            TextView url = (TextView) convertView.findViewById(R.id.page_url);
            url.setText(linkHistoryRecord.getUrl());

            TextView time = (TextView) convertView.findViewById(R.id.page_date);
            mDate.setTime(linkHistoryRecord.getTime());
            time.setText(Util.getPrettyDate(mDate));

            convertView.setTag(linkHistoryRecord);

            return convertView;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onLinkHistoryRecordChangedEvent(LinkHistoryRecord.ChangedEvent event) {
        updateListViewData();
    }
}
