package com.example.slideshow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.createchance.imageeditor.utils.Logger;
import com.createchance.imageeditor.utils.UiThreadUtil;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CHOOSE_IMAGE_FOR_EDIT = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private static final int REQUEST_CHOOSE_IMAGE_FOR_VIDEO = 3;
    private static final int REQUEST_CODE_PERMISSION = 4;
    private static final int REQUEST_CODE_IMAGE_EDIT = 5;
    private static final int REQUEST_CODE_VIDEO_GENERATE = 6;

    private RecyclerView mWorkListView;
    private WorkListAdapter mWorkListAdapter;
    private List<WorkListAdapter.WorkItem> mWorkList = new ArrayList<>();

    private WorkListAdapter.WorkItem mCurWorkItem;

    private MainSelectionWindow mVwBottomSelection;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //checkBaseDir();

        mWorkListView = findViewById(R.id.rcv_work_list);
        mWorkListAdapter = new WorkListAdapter(this, mWorkList, new WorkListAdapter.OnWorkSelectListener() {
            @Override
            public void onWorkSelected(WorkListAdapter.WorkItem workItem) {
                mCurWorkItem = workItem;
                showBottomSelectionView();
            }
        });
        mWorkListView.setLayoutManager(new LinearLayoutManager(this));
        mWorkListView.setAdapter(mWorkListAdapter);

        Log.d("click", "On Button : "+findViewById(R.id.start_slideshow));

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("click", "On Button : ");
            }
        });

        findViewById(R.id.start_slideshow).setOnClickListener(this);

        findViewById(R.id.pb_loading).setVisibility(View.VISIBLE);

        mWorkListView.setVisibility(View.GONE);
        WorkRunner.addTaskToBackground(new Runnable() {
            @Override
            public void run() {
                //initWorkList();
                UiThreadUtil.post(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.pb_loading).setVisibility(View.GONE);
                        mWorkListView.setVisibility(View.VISIBLE);
                        mWorkListAdapter.refresh(mWorkList);
                    }
                });
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_PERMISSION:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED ) {
                    Toast.makeText(this, "We need permissions to work！", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
//            case REQUEST_CHOOSE_IMAGE_FOR_EDIT:
//                if (data != null) {
//                    List<String> mediaPathList = Matisse.obtainPathResult(data);
//                    ImageEditActivity.start(this, REQUEST_CODE_IMAGE_EDIT, mediaPathList.get(0));
//                }
//                break;
//            case REQUEST_TAKE_PHOTO:
//                if (resultCode == RESULT_OK) {
//                    ImageEditActivity.start(this, REQUEST_CODE_IMAGE_EDIT, mImageFromCamera.getAbsolutePath());
//                }
//                break;
            case REQUEST_CHOOSE_IMAGE_FOR_VIDEO:
                if (data != null) {
                    List<String> mediaPathList = Matisse.obtainPathResult(data);
                    Logger.d(TAG, "onActivityResult: " + mediaPathList);
                    VideoGenerateActivity.start(this, REQUEST_CODE_VIDEO_GENERATE, mediaPathList);
                }
                break;
            case REQUEST_CODE_IMAGE_EDIT:
            case REQUEST_CODE_VIDEO_GENERATE:
                if (resultCode == RESULT_OK) {
                    // refresh work list if done.
                    findViewById(R.id.pb_loading).setVisibility(View.VISIBLE);
                    mWorkListView.setVisibility(View.GONE);
                    WorkRunner.addTaskToBackground(new Runnable() {
                        @Override
                        public void run() {
                            initWorkList();
                            UiThreadUtil.post(new Runnable() {
                                @Override
                                public void run() {
                                    findViewById(R.id.pb_loading).setVisibility(View.GONE);
                                    mWorkListView.setVisibility(View.VISIBLE);
                                    mWorkListAdapter.refresh(mWorkList);
                                }
                            });
                        }
                    });
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void chooseImages(int requestCode, int maxCount) {
        Matisse.from(MainActivity.this)
                .choose(MimeType.of(MimeType.JPEG, MimeType.PNG))
                .countable(true)
                .maxSelectable(maxCount)
                .gridExpectedSize(getResources()
                        .getDimensionPixelSize(R.dimen.grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                .theme(R.style.Matisse_Dracula)
                .thumbnailScale(0.85f)
                .imageEngine(new GlideEngine())
                .forResult(requestCode);
    }

    private void checkBaseDir() {
        if (!Constants.mBaseDir.exists()) {
            Constants.mBaseDir.mkdir();
        }
    }

    private void initWorkList() {
        if (!Constants.mBaseDir.exists()) {
            Log.e(TAG, "initWorkList, but base dir not existed!!");
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        mWorkList = new ArrayList<>();
        File[] works = Constants.mBaseDir.listFiles();
        for (File work : works) {
            WorkListAdapter.WorkItem workItem = new WorkListAdapter.WorkItem();
            workItem.mImage = work;
            workItem.mSize = work.length();
            workItem.mTimeStamp = work.lastModified();
            workItem.mFormat = work.getName().substring(work.getName().lastIndexOf(".") + 1).toUpperCase();
            if (work.getName().substring(work.getName().lastIndexOf(".") + 1).equals("mp4")) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(work.getAbsolutePath());
                workItem.mWidth = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                workItem.mHeight = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                retriever.release();
            } else {
                BitmapFactory.decodeFile(work.getAbsolutePath(), options);
                workItem.mWidth = options.outWidth;
                workItem.mHeight = options.outHeight;
            }
            mWorkList.add(workItem);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d("click", "On Button : "+v.getId());
        switch (v.getId()) {
            case R.id.start_slideshow:
                // choose image from local.
                chooseImages(REQUEST_CHOOSE_IMAGE_FOR_VIDEO, 100);
                break;
//            case R.id.tv_choose_photo:
//                chooseImages(REQUEST_CHOOSE_IMAGE_FOR_EDIT, 1);
//                break;
//            case R.id.tv_take_photo:
//                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                // Ensure that there's a camera activity to handle the intent
//                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
//                    // Create the File where the photo should go
//                    File photoFile = null;
//                    try {
//                        photoFile = createImageFile();
//                        // Continue only if the File was successfully created
//                        if (photoFile != null) {
//                            Uri photoURI = FileProvider.getUriForFile(this,
//                                    "com.createchance.imageeditordemo.fileprovider",
//                                    photoFile);
//                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
//                        }
//                    } catch (IOException ex) {
//                        // Error occurred while creating the File
//                    }
//                }
//                break;
            default:
                break;
        }
    }

    private void showBottomSelectionView() {
        if (mVwBottomSelection == null) {
            mVwBottomSelection = new MainSelectionWindow(this, new MainSelectionWindow.MainSelectionListener() {
                @Override
                public void onPreview() {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String type;
                    if (mCurWorkItem.mImage.getName().endsWith("jpg") ||
                            mCurWorkItem.mImage.getName().endsWith("png") ||
                            mCurWorkItem.mImage.getName().endsWith("webp")) {
                        type = "image/*";
                    } else {
                        type = "video/*";
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setDataAndType(FileProvider.getUriForFile(MainActivity.this,
                                "com.createchance.imageeditordemo.fileprovider",
                                mCurWorkItem.mImage), type);
                    } else {
                        intent.setDataAndType(Uri.fromFile(mCurWorkItem.mImage), type);
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                    mVwBottomSelection.dismiss();
                }

                @Override
                public void onEdit() {
                    //ImageEditActivity.start(MainActivity.this, REQUEST_CODE_IMAGE_EDIT, mCurWorkItem.mImage.getAbsolutePath());
                    mVwBottomSelection.dismiss();
                }

                @Override
                public void onShare() {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(MainActivity.this,
                                "com.createchance.imageeditordemo.fileprovider",
                                mCurWorkItem.mImage));
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mCurWorkItem.mImage));
                    }
                    intent.setType("image/*");
                    startActivity(intent);
                    mVwBottomSelection.dismiss();
                }

                @Override
                public void onDelete() {
                    // show dialog to confirm
                    DeleteConfirmDialog.start(MainActivity.this, new DeleteConfirmDialog.DeleteConformListener() {
                        @Override
                        public void onDeleteFile() {
                            mCurWorkItem.mImage.delete();
                            Toast.makeText(MainActivity.this, R.string.info_delete_done, Toast.LENGTH_SHORT).show();
                            mWorkList.remove(mCurWorkItem);
                            mCurWorkItem = null;
                            mWorkListAdapter.refresh(mWorkList);
                        }
                    });
                    mVwBottomSelection.dismiss();
                }

                @Override
                public void onCancel() {
                    mVwBottomSelection.dismiss();
                }
            });
            mVwBottomSelection.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.alpha = 1f;
                    getWindow().setAttributes(lp);
                }
            });
        }

        if (mCurWorkItem.mImage.getName().substring(mCurWorkItem.mImage.getName().lastIndexOf(".") + 1).equals("mp4")) {
            mVwBottomSelection.setEditVisible(false);
        } else {
            mVwBottomSelection.setEditVisible(true);
        }
        mVwBottomSelection.showAtLocation(findViewById(R.id.vw_root),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        // set dark background color.
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.7f;
        getWindow().setAttributes(lp);
    }
}
