package net.wendal.nutzbook.ui.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.wgs.picker.framework.ImagePicker;

import net.wendal.nutzbook.R;
import net.wendal.nutzbook.model.api.ApiClient;
import net.wendal.nutzbook.storage.LoginShared;
import net.wendal.nutzbook.storage.SettingShared;
import net.wendal.nutzbook.ui.activity.MarkdownPreviewActivity;
import net.wendal.nutzbook.util.BmpUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

public class EditorBarHandler {

    private Context context;
    private EditText edtContent;
    private InputMethodManager imm;

    public EditorBarHandler(Context context, View editorBar, EditText edtContent) {
        this.context = context;
        this.edtContent = edtContent;
        imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        ButterKnife.bind(this, editorBar);
    }

    /**
     * 加粗
     */
    @OnClick(R.id.editor_bar_btn_format_bold)
    protected void onBtnFormatBoldClick() {
        edtContent.requestFocus();
        edtContent.getText().insert(edtContent.getSelectionEnd(), "**string**");
        edtContent.setSelection(edtContent.getSelectionEnd() - 8, edtContent.getSelectionEnd() - 2);
        imm.showSoftInput(edtContent, 0);
    }

    /**
     * 倾斜
     */
    @OnClick(R.id.editor_bar_btn_format_italic)
    protected void onBtnFormatItalicClick() {
        edtContent.requestFocus();
        edtContent.getText().insert(edtContent.getSelectionEnd(), "*string*");
        edtContent.setSelection(edtContent.getSelectionEnd() - 7, edtContent.getSelectionEnd() - 1);
        imm.showSoftInput(edtContent, 0);
    }

    /**
     * 引用
     */
    @OnClick(R.id.editor_bar_btn_format_quote)
    protected void onBtnFormatQuoteClick() {
        edtContent.requestFocus();
        edtContent.getText().insert(edtContent.getSelectionEnd(), "\n\n> ");
        edtContent.setSelection(edtContent.getSelectionEnd());
    }

    /**
     * 无序列表
     */
    @OnClick(R.id.editor_bar_btn_format_list_bulleted)
    protected void onBtnFormatListBulletedClick() {
        edtContent.requestFocus();
        edtContent.getText().insert(edtContent.getSelectionEnd(), "\n\n- ");
        edtContent.setSelection(edtContent.getSelectionEnd());
    }

    /**
     * 有序列表 TODO 这里算法需要优化
     */
    @OnClick(R.id.editor_bar_btn_format_list_numbered)
    protected void onBtnFormatListNumberedClick() {
        edtContent.requestFocus();
        // 查找向上最近一个\n
        for (int n = edtContent.getSelectionEnd() - 1; n >= 0; n--) {
            char c = edtContent.getText().charAt(n);
            if (c == '\n') {
                try {
                    int index = Integer.parseInt(edtContent.getText().charAt(n + 1) + "");
                    if (edtContent.getText().charAt(n + 2) == '.' && edtContent.getText().charAt(n + 3) == ' ') {
                        edtContent.getText().insert(edtContent.getSelectionEnd(), "\n\n" + (index + 1) + ". ");
                        return;
                    }
                } catch (Exception e) {
                    // TODO 这里有问题是如果数字超过10，则无法检测，未来逐渐优化
                }
            }
        }
        // 没找到
        edtContent.getText().insert(edtContent.getSelectionEnd(), "\n\n1. ");
        edtContent.setSelection(edtContent.getSelectionEnd());
    }

    /**
     * 插入代码
     */
    @OnClick(R.id.editor_bar_btn_insert_code)
    protected void onBtnInsertCodeClick() {
        edtContent.requestFocus();
        edtContent.getText().insert(edtContent.getSelectionEnd(), "\n\n```\n\n```\n ");
        edtContent.setSelection(edtContent.getSelectionEnd() - 6);
    }

    /**
     * 插入链接
     */
    @OnClick(R.id.editor_bar_btn_insert_link)
    protected void onBtnInsertLinkClick() {
        new MaterialDialog.Builder(context)
                .iconRes(R.drawable.ic_insert_link_grey600_24dp)
                .title(R.string.add_link)
                .customView(R.layout.dialog_tool_insert_link, false)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {

                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        View view = dialog.getCustomView();
                        EditText edtTitle = ButterKnife.findById(view, R.id.dialog_tool_insert_link_edt_title);
                        EditText edtLink = ButterKnife.findById(view, R.id.dialog_tool_insert_link_edt_link);

                        String insertText = " [" + edtTitle.getText() + "](" + edtLink.getText() + ") ";
                        edtContent.requestFocus();
                        edtContent.getText().insert(edtContent.getSelectionEnd(), insertText);
                    }

                })
                .show();
    }

    /**
     * 插入图片 TODO 图片上传接口使用第三方实现
     */
    @OnClick(R.id.editor_bar_btn_insert_photo)
    protected void onBtnInsertPhotoClick() {
        insertPhotoDefaultAction();
    }

    private void insertPhotoDefaultAction() {
        //一次只允许上传一张
        ImagePicker.build(context, new ImagePicker.PickListener() {
            @Override
            public void onPickedSuccessfully(ArrayList<String> images) {
                if(images.size() > 0){

                    final MaterialDialog dialog = new MaterialDialog.Builder(context)
                            .title("上传中...")
                            .progress(true, 0)
                            .cancelable(false)
                            .build();
                    dialog.show();

                    File file = new File(images.get(0));
                    if(file.isFile() && file.exists()){
                        //压缩图片
                        String newFilePath = BmpUtils.revisionImageSize(file.getAbsolutePath());
                        File compressedFile = new File(newFilePath);
                        //得到mime
                        String mime = MimeTypeMap.getFileExtensionFromUrl(images.get(0));
                        TypedFile tf = new TypedFile("image/" + mime, compressedFile);
                        //开始上传
                        ApiClient.service.uploadImage(LoginShared.getAccessToken(context), tf, new Callback<Map<String, String>>(){

                            @Override
                            public void success(Map<String, String> result, Response response) {
                                //清除临时目录
                                BmpUtils.deleteTempDir();
                                dialog.dismiss();

                                String url = result.get("url");
                                //得到url, 插入到edit
                                if(url != null){
                                    edtContent.requestFocus();
                                    edtContent.getText().insert(edtContent.getSelectionEnd(), " ![Image](" + url + ") ");
                                    imm.showSoftInput(edtContent, 0);
                                }
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                //清除临时目录
                                BmpUtils.deleteTempDir();

                                Toast.makeText(context, "图片上传失败:" + error.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancel() {}
        }, 1).startActivity();

    }

    /**
     * 预览
     */
    @OnClick(R.id.editor_bar_btn_preview)
    protected void onBtnPreviewClick() {
        String content = edtContent.getText().toString();
        if (SettingShared.isEnableTopicSign(context)) { // 添加小尾巴
            content += "\n\n" + SettingShared.getTopicSignContent(context);
        }

        Intent intent = new Intent(context, MarkdownPreviewActivity.class);
        intent.putExtra("markdownText", content);
        context.startActivity(intent);
    }

}
