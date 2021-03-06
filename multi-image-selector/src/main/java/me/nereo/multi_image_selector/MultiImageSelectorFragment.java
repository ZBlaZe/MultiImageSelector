package me.nereo.multi_image_selector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.nereo.multi_image_selector.adapter.FolderAdapter;
import me.nereo.multi_image_selector.adapter.ImageGridAdapter;
import me.nereo.multi_image_selector.bean.Folder;
import me.nereo.multi_image_selector.bean.Image;
import me.nereo.multi_image_selector.utils.FileUtils;
import me.nereo.multi_image_selector.utils.TimeUtils;

/**
 * Image Select Fragment
 * Created by Nereo on 2015/4/7.
 */
public class MultiImageSelectorFragment extends Fragment {

    private static final String TAG = "MultiImageSelector";

    /**
     * Maximum picture selection times, int type
     */
    public static final String EXTRA_SELECT_COUNT = "max_select_count";

    /**
     * Photos Select mode, int type
     */
    public static final String EXTRA_SELECT_MODE = "select_count_mode";
    /**
     * Whether to display the camera, boolean type
     */
    public static final String EXTRA_SHOW_CAMERA = "show_camera";
    /**
     * The default selection of the data set
     */
    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_result";
    /**
     * The default sort order id
     */
    public static final String EXTRA_SORT_ORDER_ID = "sort_order_id";
    /**
     * Radio
     */
    public static final int MODE_SINGLE = 0;
    /**
     * Multiple choice
     */
    public static final int MODE_MULTI = 1;
    // Different definitions loader
    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;
    // Request loading system camera
    private static final int REQUEST_CAMERA = 100;


    // Results
    private ArrayList<String> resultList = new ArrayList<>();
    // Folder Data
    private ArrayList<Folder> mResultFolder = new ArrayList<>();

    // Image Grid
    private GridView mGridView;
    private Callback mCallback;

    private ImageGridAdapter mImageAdapter;
    private FolderAdapter mFolderAdapter;

    private ListPopupWindow mFolderPopupWindow;

    // Timeline
    private TextView mIndicatorLineText;
    // Category
    private TextView mCategoryText;
    // Preview button
    private Button mPreviewBtn;
    // Bottom View
    private View mPopupAnchorView;

    private int mDesireImageCount;

    private boolean mIsShowCamera = false;

    private int mGridWidth, mGridHeight;

    private File mTmpFile;

    public int mCurrentSortOrderId;
    private Folder mLastSelectedFolder;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (Callback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("The Activity must implement MultiImageSelectorFragment.Callback interface...");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multi_image, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        // Select the number of pictures
        mDesireImageCount = args.getInt(EXTRA_SELECT_COUNT);

        // Image Select mode
        final int mode = args.getInt(EXTRA_SELECT_MODE);

        // The default selection
        if (mode == MODE_MULTI) {
            ArrayList<String> tmp = args.getStringArrayList(EXTRA_DEFAULT_SELECTED_LIST);
            if (tmp != null && tmp.size() > 0) {
                resultList = tmp;
            }
        }

        // Sort order id
        mCurrentSortOrderId = args.getInt(EXTRA_SORT_ORDER_ID);

        // Whether to display the camera
        mIsShowCamera = args.getBoolean(EXTRA_SHOW_CAMERA, true);
        mImageAdapter = new ImageGridAdapter(getActivity(), mIsShowCamera);
        // Choose whether to display indicator
        mImageAdapter.showSelectIndicator(mode == MODE_MULTI);

        mPopupAnchorView = view.findViewById(R.id.footer);

        mIndicatorLineText = (TextView) view.findViewById(R.id.indicator_area);
        // Initialization, first hide the current timeline
        mIndicatorLineText.setVisibility(View.GONE);

        mCategoryText = (TextView) view.findViewById(R.id.category_btn);
        // Initialization, load all pictures
        mCategoryText.setText(R.string.folder_all);
        mCategoryText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFolderPopupWindow == null) {
                    createPopupFolderList(mGridWidth, mGridHeight);
                }

                if (mFolderPopupWindow.isShowing()) {
                    mFolderPopupWindow.dismiss();
                } else {
                    mFolderPopupWindow.show();
                    int index = mFolderAdapter.getSelectIndex();
                    index = index == 0 ? index : index - 1;
                    mFolderPopupWindow.getListView().setSelection(index);
                }
            }
        });

        mPreviewBtn = (Button) view.findViewById(R.id.preview);
        // Initialization, the button state initialization
        if (resultList == null || resultList.size() <= 0) {
            mPreviewBtn.setText(R.string.preview);
            mPreviewBtn.setEnabled(false);
        }
        mPreviewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Preview
            }
        });

        mGridView = (GridView) view.findViewById(R.id.grid);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int state) {

                final Picasso picasso = Picasso.with(getActivity());
                if (state == SCROLL_STATE_IDLE || state == SCROLL_STATE_TOUCH_SCROLL) {
                    picasso.resumeTag(getActivity());
                } else {
                    picasso.pauseTag(getActivity());
                }

                if (state == SCROLL_STATE_IDLE) {
                    // Stop slide, the indicator disappears
                    mIndicatorLineText.setVisibility(View.GONE);
                } else if (state == SCROLL_STATE_FLING) {
                    mIndicatorLineText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mIndicatorLineText.getVisibility() == View.VISIBLE) {
                    int index = firstVisibleItem + 1 == view.getAdapter().getCount() ? view.getAdapter().getCount() - 1 : firstVisibleItem + 1;
                    Image image = (Image) view.getAdapter().getItem(index);
                    if (image != null) {
                        mIndicatorLineText.setText(getActualIndicatorParameter(image));
                    }
                }
            }
        });
        mGridView.setAdapter(mImageAdapter);
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int width = mGridView.getWidth();
                final int height = mGridView.getHeight();

                mGridWidth = width;
                mGridHeight = height;

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                final int numCount = width / desireSize;
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (width - columnSpace * (numCount - 1)) / numCount;
                mImageAdapter.setItemSize(columnWidth);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mImageAdapter.isShowCamera()) {
                    // If the camera, the first camera is displayed as a Grid to address the special logic
                    if (i == 0) {
                        showCameraAction();
                    } else {
                        // Normal operation
                        Image image = (Image) adapterView.getAdapter().getItem(i);
                        selectImageFromGrid(image, mode);
                    }
                } else {
                    // Normal operation
                    Image image = (Image) adapterView.getAdapter().getItem(i);
                    selectImageFromGrid(image, mode);
                }
            }
        });

        mFolderAdapter = new FolderAdapter(getActivity());
    }

    private String getActualIndicatorParameter(Image image) {
        switch (mCurrentSortOrderId) {
            // Name
            case 0:
            case 1:
                return image.name.substring(0, 1);
            // Date created
            case 2:
            case 3:
                return TimeUtils.formatPhotoDate(image.timeAdded);
            // Date modified
            case 4:
            case 5:
                return TimeUtils.formatPhotoDate(image.timeModified);
            // Size
            case 6:
            case 7:
                return String.format("%s bytes", image.size);
            default:
                return "";
        }

    }

    /**
     * Create popup ListView
     */
    private void createPopupFolderList(int width, int height) {
        mFolderPopupWindow = new ListPopupWindow(getActivity());
        mFolderPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mFolderPopupWindow.setAdapter(mFolderAdapter);
        mFolderPopupWindow.setContentWidth(width);
        mFolderPopupWindow.setWidth(width);
        mFolderPopupWindow.setHeight(height * 5 / 8);
        mFolderPopupWindow.setAnchorView(mPopupAnchorView);
        mFolderPopupWindow.setModal(true);
        mFolderPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mFolderAdapter.setSelectIndex(i);

                final int index = i;
                final AdapterView v = adapterView;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFolderPopupWindow.dismiss();
                        setDataForCategory(index, v);
                        // Slide to the initial position
                        mGridView.smoothScrollToPosition(0);
                    }
                }, 100);

            }
        });
    }

    private void setDataForCategory(int index, AdapterView v) {
        mLastSelectedFolder = null;
        if (index == 0) {
            showAllImages();
        } else {
            mLastSelectedFolder = (Folder) v.getAdapter().getItem(index);
            showImagesFromFolder(mLastSelectedFolder);
        }
    }

    private void showAllImages() {
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
        mCategoryText.setText(R.string.folder_all);
        mImageAdapter.setShowCamera(mIsShowCamera);
    }

    private void showImagesFromFolder(Folder folder) {
        if (null != folder) {
            mImageAdapter.setData(folder.images);
            mCategoryText.setText(folder.name);
            // Set the default selection
            if (resultList != null && resultList.size() > 0) {
                mImageAdapter.setDefaultSelected(resultList);
            }
        }
        mImageAdapter.setShowCamera(false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // First load all pictures
        //new LoadImageTask().execute();
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // After completion of the camera to take pictures, return image path
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTmpFile != null && mCallback != null) {
                    mCallback.onCameraShot(mTmpFile);
                }
            } else {
                if (mTmpFile != null && mTmpFile.exists()) {
                    mTmpFile.delete();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "on change");

        if (mFolderPopupWindow != null && mFolderPopupWindow.isShowing()) {
            mFolderPopupWindow.dismiss();
        }
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onGlobalLayout() {

                final int height = mGridView.getHeight();

                final int desireSize = getResources().getDimensionPixelOffset(R.dimen.image_size);
                Log.d(TAG, "Desire Size = " + desireSize);
                final int numCount = mGridView.getWidth() / desireSize;
                Log.d(TAG, "Grid Size = " + mGridView.getWidth());
                Log.d(TAG, "num count = " + numCount);
                final int columnSpace = getResources().getDimensionPixelOffset(R.dimen.space_size);
                int columnWidth = (mGridView.getWidth() - columnSpace * (numCount - 1)) / numCount;
                mImageAdapter.setItemSize(columnWidth);

                if (mFolderPopupWindow != null) {
                    mFolderPopupWindow.setHeight(height * 5 / 8);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        super.onConfigurationChanged(newConfig);

    }

    /**
     * Choose camera
     */
    private void showCameraAction() {
        // Skip to system cameras
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Set the camera to take pictures after the system output path
            // Create a temporary file
            mTmpFile = FileUtils.createTmpFile(getActivity());
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTmpFile));
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        } else {
            Toast.makeText(getActivity(), R.string.msg_no_camera, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Select Image Operation
     *
     * @param image
     */
    private void selectImageFromGrid(Image image, int mode) {
        if (image == null)
            return;
        // Multiple choice mode
        if (mode == MODE_MULTI) {
            if (resultList.contains(image.path)) {
                resultList.remove(image.path);
                if (resultList.size() != 0) {
                    mPreviewBtn.setEnabled(true);
                    mPreviewBtn.setText(getResources().getString(R.string.preview) + "(" + resultList.size() + ")");
                } else {
                    mPreviewBtn.setEnabled(false);
                    mPreviewBtn.setText(R.string.preview);
                }
                if (mCallback != null) {
                    mCallback.onImageUnselected(image.path);
                }
            } else {
                // Analyzing choose the number of questions
                if (mDesireImageCount == resultList.size()) {
                    Toast.makeText(getActivity(), R.string.msg_amount_limit, Toast.LENGTH_SHORT).show();
                    return;
                }

                resultList.add(image.path);
                mPreviewBtn.setEnabled(true);
                mPreviewBtn.setText(getResources().getString(R.string.preview) + "(" + resultList.size() + ")");
                if (mCallback != null) {
                    mCallback.onImageSelected(image.path);
                }
            }
            mImageAdapter.select(image);
        } else if (mode == MODE_SINGLE) {
            // Radio mode
            if (mCallback != null) {
                mCallback.onSingleImageSelected(image.path);
            }
        }
    }

    /**
     * Change sorting
     *
     * @param sortOrderId
     */
    public void setSortOrder(int sortOrderId) {
        mCurrentSortOrderId = sortOrderId;
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
        mGridView.setSelection(0);
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media._ID};

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String sortOrder = IMAGE_PROJECTION[mCurrentSortOrderId / 2 + 1] + (mCurrentSortOrderId % 2 == 0 ? "" : " DESC");
            if (id == LOADER_ALL) {
                return new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        null, null, sortOrder);
            } else if (id == LOADER_CATEGORY) {
                return new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[0] + " like '%" + args.getString("path") + "%'", null, sortOrder);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                List<Image> images = new ArrayList<>();
                if(data.getCount() == 0)
                    return;
                mResultFolder.clear();
                data.moveToFirst();
                do {
                    String path     = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                    String name     = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                    long dateAdded  = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
                    long dateModify = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
                    long size       = data.getLong(data.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));
                    Image image = new Image(path, name, dateAdded, dateModify, size);
                    images.add(image);
                    // Regenerate folders
                        // Get folder name
                    File imageFile = new File(path);
                    File folderFile = imageFile.getParentFile();
                    Folder folder = new Folder(
                            folderFile.getName(),
                            folderFile.getAbsolutePath(),
                            image
                    );
                    if (!mResultFolder.contains(folder)) {
                        List<Image> imageList = new ArrayList<>();
                        imageList.add(image);
                        folder.images = imageList;
                        mResultFolder.add(folder);
                    } else {
                        // Update
                        Folder f = mResultFolder.get(mResultFolder.indexOf(folder));
                        f.images.add(image);
                    }
                } while (data.moveToNext());

                mImageAdapter.setData(images);

                // Set the default selection
                if (resultList != null && resultList.size() > 0) {
                    mImageAdapter.setDefaultSelected(resultList);
                }

                mFolderAdapter.setData(mResultFolder);

                if(mLastSelectedFolder != null) {
                    mLastSelectedFolder = updateFolderData(mLastSelectedFolder);
                    showImagesFromFolder(mLastSelectedFolder);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    private Folder updateFolderData(Folder targetFolder) {
        for (Folder curFolder : mResultFolder) {
            if(targetFolder.equals(curFolder))
                return curFolder;
        }
        return null;
    }

    /**
     * Callback Interface
     */
    public interface Callback {
        void onSingleImageSelected(String path);

        void onImageSelected(String path);

        void onImageUnselected(String path);

        void onCameraShot(File imageFile);
    }
}
