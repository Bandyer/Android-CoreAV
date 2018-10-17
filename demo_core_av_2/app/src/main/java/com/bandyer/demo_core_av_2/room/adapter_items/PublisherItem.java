/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.room.adapter_items;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bandyer.core_av.OnStreamListener;
import com.bandyer.core_av.Stream;
import com.bandyer.core_av.capturer.AbstractBaseCapturer;
import com.bandyer.core_av.capturer.CapturerAV;
import com.bandyer.core_av.capturer.video.CapturerVideo;
import com.bandyer.core_av.publisher.Publisher;
import com.bandyer.core_av.publisher.RecordingException;
import com.bandyer.core_av.publisher.RecordingListener;
import com.bandyer.core_av.view.OnViewStatusListener;
import com.bandyer.core_av.view.StreamView;
import com.bandyer.core_av.view.VideoStreamView;
import com.bandyer.demo_core_av_2.R;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.viven.imagezoom.ImageZoomHelper;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;

import static android.view.View.GONE;

/**
 * @author kristiyan
 **/

public class PublisherItem extends AbstractItem<PublisherItem, PublisherItem.ViewHolder> {

    private Publisher publisher;
    private AbstractBaseCapturer capturerAV;

    public PublisherItem(Publisher publisher, AbstractBaseCapturer capturerAV) {
        this.publisher = publisher;
        this.capturerAV = capturerAV;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    @Override
    public long getIdentifier() {
        return publisher.getId().hashCode();
    }

    @Override
    public int getType() {
        return R.id.publisher_item;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.item_publisher;
    }

    @NonNull
    @Override
    public PublisherItem.ViewHolder getViewHolder(@NonNull View v) {
        return new PublisherItem.ViewHolder(v);
    }

    /**
     * our ViewHolder
     */
    public static class ViewHolder extends ButterKnifeViewHolder<PublisherItem> {

        @BindView(R.id.preview)
        VideoStreamView preview;

        @BindView(R.id.stream_id)
        TextView streamId;

        @BindView(R.id.micButton)
        AppCompatImageButton micButton;

        @BindView(R.id.videoButton)
        AppCompatImageButton videoButton;

        @BindView(R.id.switchCameraButton)
        AppCompatImageButton switchCameraButton;

        @BindView(R.id.fpsQuality)
        TextView fpsQualityTextView;

        @BindView(R.id.videoQuality)
        TextView videoQualityTextView;

        @BindView(R.id.recordButton)
        AppCompatImageButton recordButton;


        boolean audioMuted = false;
        boolean videoMuted = false;

        ViewHolder(View itemView) {
            super(itemView);
            ImageZoomHelper.setViewZoomable(preview);
        }

        AbstractBaseCapturer capturerAV;

        Publisher publisher;

        @Override
        public void bindView(@NonNull final PublisherItem item, @NonNull List<Object> payloads) {
            capturerAV = item.capturerAV;
            publisher = item.publisher;

            preview.setViewListener(new OnViewStatusListener() {

                @Override
                public void onViewSizeChanged(int width, int height, int rotationDegree) {
                    Log.e("PubView", "w " + width + " h " + height + "r " + rotationDegree);
                }

                @Override
                public void onFirstFrameRendered() {
                    Log.e("PubView", "frameRendered");
                }
            });

            item.publisher.setView(preview, new OnStreamListener() {
                @Override
                public void onReadyToPlay(@NotNull StreamView view, @NonNull Stream stream) {
                    updateAudioVideoButton(stream.getHasVideo(),
                            stream.getHasAudio(),
                            stream.isAudioMuted(),
                            stream.isVideoMuted());
                    view.play(stream);
                }
            });

            streamId.setText(item.publisher.getId());
            streamId.setSelected(true);
        }

        @Override
        public void unbindView(@NonNull PublisherItem item) {
            item.publisher.releaseView();
            streamId.setText(null);
            switchCameraButton.setVisibility(View.VISIBLE);
            videoButton.setVisibility(View.VISIBLE);
            micButton.setVisibility(View.VISIBLE);
            audioMuted = false;
            videoMuted = false;
            capturerAV = null;
            publisher = null;
            listener = null;
        }

        void updateAudioVideoButton(Boolean previewHasVideo, Boolean previewHasAudio, Boolean audioMuted, Boolean videoMuted) {
            this.audioMuted = audioMuted;
            this.videoMuted = videoMuted;
            if (!previewHasVideo) {
                videoButton.setVisibility(GONE);
                switchCameraButton.setVisibility(GONE);
            }

            if (!previewHasAudio)
                micButton.setVisibility(GONE);

            micButton.setImageResource(audioMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
            videoButton.setImageResource(videoMuted ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        }


        @OnClick(R.id.micButton)
        void onAudioToggle(AppCompatImageButton view) {
            audioMuted = !audioMuted;
            view.setImageResource(audioMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
            preview.audioEnabled(!audioMuted);
        }

        @OnClick(R.id.videoButton)
        void onVideoToggle(AppCompatImageButton view) {
            videoMuted = !videoMuted;
            view.setImageResource(videoMuted ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
            preview.videoEnabled(!videoMuted);
        }

        @OnClick(R.id.switchCameraButton)
        void onSwitchCamera(AppCompatImageButton view) {
            if (capturerAV instanceof CapturerVideo) {
                ((CapturerVideo) capturerAV).switchVideoFeeder();
            } else if (capturerAV instanceof CapturerAV) {
                ((CapturerAV) capturerAV).switchVideoFeeder();
            }
        }

        @OnClick(R.id.recordButton)
        void onRecord(final AppCompatImageButton view) {
            if (publisher.isRecording()) publisher.stopRecording(listener);
            else publisher.startRecording(listener);
        }

        RecordingListener listener = new RecordingListener() {

            @Override
            public void onSuccess(@NotNull String recordId, boolean isRecording) {
                String message = isRecording ? "Started" : "Stopped";
                Toast.makeText(preview.getContext(), message + " recording", Toast.LENGTH_SHORT).show();
                ImageViewCompat.setImageTintList(recordButton, ColorStateList.valueOf(isRecording ? Color.RED : Color.WHITE));
            }

            @Override
            public void onError(@Nullable String recordId, boolean isRecording, @NotNull RecordingException reason) {
                ImageViewCompat.setImageTintList(recordButton, ColorStateList.valueOf(isRecording ? Color.RED : Color.WHITE));
            }
        };
    }


    public static class PublisherItemClickListener extends ClickEventHook<PublisherItem> {

        @Nullable
        @Override
        public View onBind(RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof PublisherItem.ViewHolder)
                return ((PublisherItem.ViewHolder) viewHolder).itemView;
            return null;
        }

        @Override
        public void onClick(@NonNull View v, int position, @NonNull FastAdapter<PublisherItem> fastAdapter, @NonNull PublisherItem item) {
            GridLayout pub_options = v.findViewById(R.id.publishing_options);
            pub_options.setVisibility(pub_options.getVisibility() == View.VISIBLE ? GONE : View.VISIBLE);
        }
    }

}