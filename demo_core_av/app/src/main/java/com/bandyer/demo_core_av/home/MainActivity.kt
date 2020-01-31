/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */
package com.bandyer.demo_core_av.home

import android.Manifest
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import com.bandyer.demo_core_av.BaseActivity
import com.bandyer.demo_core_av.R
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPicker
import com.bandyer.demo_core_av.design.bottom_sheet.picker.BottomListPickerItem
import com.bandyer.demo_core_av.networking.ApiUtils
import com.bandyer.demo_core_av.networking.request_models.RequestToken
import com.bandyer.demo_core_av.room.AutoPubSubRoomActivity
import com.bandyer.demo_core_av.room.RoomActivity
import com.google.android.material.snackbar.Snackbar
import io.ghyeok.stickyswitch.widget.StickySwitch
import io.ghyeok.stickyswitch.widget.StickySwitch.OnSelectedChangeListener
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * @author kristiyan
 */
@RuntimePermissions
class MainActivity : BaseActivity() {

    private var token: String? = null
    private var autoPubSub = true
    private var muteAllAudio = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        room_url!!.setText(BASE_URL)
        room_type!!.onSelectedChangeListener = object : OnSelectedChangeListener {
            override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                requestToken.type = if (direction == StickySwitch.Direction.LEFT) "p2p" else "mtm"
            }
        }
        button_role.setOnClickListener {
            val rolePicker = BottomListPicker<String>(this, getString(R.string.choose_role))
            val roles = listOf("presenter", "viewer", "publishOnly", "viewerWithData")
            rolePicker.setItems(roles, roles.indexOf(requestToken.role), object : BottomListPickerItem.Delegate<String> {
                override fun onClicked(adapterPosition: Int, item: String?) {
                    button_role!!.text = item
                    requestToken.role = item!!
                    rolePicker.dismiss()
                }
            }).show()
        }

        auto_pub_sub_switch.setOnCheckedChangeListener { buttonView, isChecked -> autoPubSub = isChecked }
        mute_audio.setOnCheckedChangeListener { buttonView, isChecked -> muteAllAudio = isChecked }
        button_enter.setOnClickListener { joinRoom() }
    }

    override fun onDestroy() {
        super.onDestroy()
        room_type!!.onSelectedChangeListener = null
    }

    private fun joinRoom() {
        if (!Patterns.WEB_URL.matcher(room_url!!.text.toString()).matches()) {
            room_url_input!!.isErrorEnabled = true
            room_url_input!!.error = "Url not valid."
            return
        }
        ApiUtils.getRestApi(room_url!!.text.toString()).connect(requestToken).enqueue(object : Callback<String?> {
            override fun onResponse(call: Call<String?>, response: Response<String?>) {
                token = response.body()
                if (token != null) showCameraWithPermissionCheck() else showError("RoomToken Request", "Token is null!!")
            }

            override fun onFailure(call: Call<String?>, t: Throwable) {
                showError("RoomToken Request", t.message)
            }
        })
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    fun showCamera() {
        if (autoPubSub) AutoPubSubRoomActivity.Companion.show(this, token, muteAllAudio) else RoomActivity.Companion.show(this, token, muteAllAudio)
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    fun showRationaleForCamera(request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_allow) { dialog, which -> request.proceed() }
                .setNegativeButton(R.string.button_deny) { dialog, which -> request.cancel() }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    fun showDeniedForCamera() {
        Snackbar.make(room_type!!, R.string.permission_camera_denied, Snackbar.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    fun showNeverAskForCamera() {
        Snackbar.make(room_type!!, R.string.permission_camera_never_ask_again, Snackbar.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated method
        onRequestPermissionsResult(requestCode, grantResults)
    }

    companion object {
        private val requestToken = RequestToken()
        private const val BASE_URL = "https://develop.bandyer.com:3004/"
    }
}