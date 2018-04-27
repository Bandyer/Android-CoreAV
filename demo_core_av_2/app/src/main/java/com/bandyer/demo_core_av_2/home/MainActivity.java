/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.home;

import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.util.Patterns;
import android.widget.CompoundButton;

import com.bandyer.demo_core_av_2.BaseActivity;
import com.bandyer.demo_core_av_2.R;
import com.bandyer.demo_core_av_2.design.bottom_sheet.picker.BottomListPicker;
import com.bandyer.demo_core_av_2.design.bottom_sheet.picker.BottomListPickerItem;
import com.bandyer.demo_core_av_2.networking.ApiUtils;
import com.bandyer.demo_core_av_2.networking.request_models.RequestToken;
import com.bandyer.demo_core_av_2.room.AutoPubSubRoomActivity;
import com.bandyer.demo_core_av_2.room.RoomActivity;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import io.ghyeok.stickyswitch.widget.StickySwitch;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import retrofit2.Call;
import retrofit2.Callback;

import static android.support.design.widget.Snackbar.LENGTH_SHORT;

/**
 * @author kristiyan
 **/
@RuntimePermissions
public class MainActivity extends BaseActivity {

    private static final RequestToken requestToken = new RequestToken();

    private static final String BASE_URL = "https://develop.bandyer.com:3004/";

    @BindView(R.id.room_url_input)
    TextInputLayout roomUrlInput;

    @BindView(R.id.room_url)
    TextInputEditText roomUrl;

    @BindView(R.id.button_role)
    AppCompatButton buttonRole;

    @BindView(R.id.room_type)
    StickySwitch roomType;

    private String token;

    private boolean autoPubSub = true;

    private boolean muteAllAudio = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomUrl.setText(BASE_URL);
        roomType.setOnSelectedChangeListener(new StickySwitch.OnSelectedChangeListener() {
            @Override
            public void onSelectedChange(StickySwitch.Direction direction, String s) {
                requestToken.type = direction == StickySwitch.Direction.LEFT ? "p2p" : "mtm";
            }
        });
    }

    @OnClick(R.id.button_role)
    public void onChangeRole() {
        final BottomListPicker<String> rolePicker = new BottomListPicker<>(this, getString(R.string.choose_role));
        List<String> roles = Arrays.asList("presenter", "viewer", "publishOnly", "viewerWithData");
        rolePicker.setItems(roles, roles.indexOf(requestToken.role), new BottomListPickerItem.Delegate<String>() {
            @Override
            public void onClicked(int adapterPosition, String item) {
                buttonRole.setText(item);
                requestToken.role = item;
                rolePicker.dismiss();
            }
        }).show();
    }

    @OnCheckedChanged(R.id.auto_pub_sub_switch)
    public void onAutoPubSub(CompoundButton switcher, boolean isChecked) {
        autoPubSub = isChecked;
    }

    @OnCheckedChanged(R.id.mute_audio)
    public void onRoomAudioMuted(CompoundButton switcher, boolean isChecked) {
        muteAllAudio = isChecked;
    }

    @OnClick(R.id.button_enter)
    public void joinRoom() {

        if (!Patterns.WEB_URL.matcher(roomUrl.getText().toString()).matches()) {
            roomUrlInput.setErrorEnabled(true);
            roomUrlInput.setError("Url not valid.");
            return;
        }

        ApiUtils.getRestApi(roomUrl.getText().toString()).connect(requestToken).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, final @NonNull retrofit2.Response<String> response) {
                token = response.body();
                if (token != null)
                    MainActivityPermissionsDispatcher.showCameraWithPermissionCheck(MainActivity.this);
                else
                    showError("RoomToken Request", "Token is null!!");
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showError("RoomToken Request", t.getMessage());
            }
        });
    }

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCamera() {
        if (autoPubSub)
            AutoPubSubRoomActivity.show(this, token, muteAllAudio);
        else
            RoomActivity.show(this, token, muteAllAudio);

    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showDeniedForCamera() {
        Snackbar.make(roomType, R.string.permission_camera_denied, LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showNeverAskForCamera() {
        Snackbar.make(roomType, R.string.permission_camera_never_ask_again, LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

}
