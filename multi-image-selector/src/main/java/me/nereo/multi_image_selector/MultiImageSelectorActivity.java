package me.nereo.multi_image_selector;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.File;
import java.util.ArrayList;

/**
 * Multi-pattern selection
 * Created by Nereo on 2015/4/7.
 */
public class MultiImageSelectorActivity extends FragmentActivity implements MultiImageSelectorFragment.Callback {

    /**
     * The maximum picture selection times, int type, default 9
     */
    public static final String EXTRA_SELECT_COUNT = "max_select_count";
    /**
     * Picture Select mode, the default multiple choice
     */
    public static final String EXTRA_SELECT_MODE = "select_count_mode";
    /**
     * Whether to display the camera, the default display
     */
    public static final String EXTRA_SHOW_CAMERA = "show_camera";
    /**
     * Choose a result, the return for the ArrayList & lt; String & gt; image path set
     */
    public static final String EXTRA_RESULT = "select_result";
    /**
     * The default selection set
     */
    public static final String EXTRA_DEFAULT_SELECTED_LIST = "default_list";

    /**
     * Radio
     */
    public static final int MODE_SINGLE = 0;
    /**
     * Multiple choice
     */
    public static final int MODE_MULTI = 1;

    private ArrayList<String> resultList = new ArrayList<>();
    private Button mSubmitBtn;
    private ImageButton mSortBtn;
    private int mDefaultCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default);

        Intent intent = getIntent();
        mDefaultCount = intent.getIntExtra(EXTRA_SELECT_COUNT, 9);
        int mode = intent.getIntExtra(EXTRA_SELECT_MODE, MODE_MULTI);
        boolean isShowCamera = intent.getBooleanExtra(EXTRA_SHOW_CAMERA, true);
        if (mode == MODE_MULTI && intent.hasExtra(EXTRA_DEFAULT_SELECTED_LIST)) {
            resultList = intent.getStringArrayListExtra(EXTRA_DEFAULT_SELECTED_LIST);
        }
        addViewerFragment(mode, isShowCamera);
        initButtons();
    }

    private void addViewerFragment(int mode, boolean isShowCamera) {
        Bundle bundle = new Bundle();
        bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_COUNT, mDefaultCount);
        bundle.putInt(MultiImageSelectorFragment.EXTRA_SELECT_MODE, mode);
        bundle.putBoolean(MultiImageSelectorFragment.EXTRA_SHOW_CAMERA, isShowCamera);
        bundle.putStringArrayList(MultiImageSelectorFragment.EXTRA_DEFAULT_SELECTED_LIST, resultList);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.image_grid, Fragment.instantiate(this, MultiImageSelectorFragment.class.getName(), bundle))
                .commit();
    }

    private void initButtons() {
        // Back button
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        // Submit button
        mSubmitBtn = (Button) findViewById(R.id.commit);
        if (resultList == null || resultList.size() <= 0) {
            mSubmitBtn.setText(getString(R.string.choose));
            mSubmitBtn.setEnabled(false);
        } else {
            mSubmitBtn.setText(getString(R.string.choose) + "(" + resultList.size() + "/" + mDefaultCount + ")");
            mSubmitBtn.setEnabled(true);
        }
        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (resultList != null && resultList.size() > 0) {
                    // Returns the selected image data
                    Intent data = new Intent();
                    data.putStringArrayListExtra(EXTRA_RESULT, resultList);
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });
        // Sort button
        mSortBtn = (ImageButton) findViewById(R.id.btn_sort);
        mSortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 19.01.2016 Implement sorting
            }
        });
    }

    @Override
    public void onSingleImageSelected(String path) {
        Intent data = new Intent();
        resultList.add(path);
        data.putStringArrayListExtra(EXTRA_RESULT, resultList);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onImageSelected(String path) {
        if (!resultList.contains(path)) {
            resultList.add(path);
        }
        // After picture selected, change button states
        if (resultList.size() > 0) {
            mSubmitBtn.setText(getString(R.string.choose) + "(" + resultList.size() + "/" + mDefaultCount + ")");
            if (!mSubmitBtn.isEnabled()) {
                mSubmitBtn.setEnabled(true);
            }
        }
    }

    @Override
    public void onImageUnselected(String path) {
        if (resultList.contains(path)) {
            resultList.remove(path);
            mSubmitBtn.setText(getString(R.string.choose) + "(" + resultList.size() + "/" + mDefaultCount + ")");
        } else {
            mSubmitBtn.setText(getString(R.string.choose) + "(" + resultList.size() + "/" + mDefaultCount + ")");
        }
        // When no one picture selected, change button states
        if (resultList.size() == 0) {
            mSubmitBtn.setText(getString(R.string.choose));
            mSubmitBtn.setEnabled(false);
        }
    }

    @Override
    public void onCameraShot(File imageFile) {
        if (imageFile != null) {
            Intent data = new Intent();
            resultList.add(imageFile.getAbsolutePath());
            data.putStringArrayListExtra(EXTRA_RESULT, resultList);
            setResult(RESULT_OK, data);
            finish();
        }
    }
}
