/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_core_av_2.room.adapter_items;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.bandyer.core_av.OnStreamListener;
import com.bandyer.core_av.Stream;
import com.bandyer.core_av.subscriber.Subscriber;
import com.bandyer.core_av.subscriber.VideoFpsQuality;
import com.bandyer.core_av.subscriber.VideoQuality;
import com.bandyer.core_av.subscriber.VideoResolutionQuality;
import com.bandyer.core_av.view.BandyerView;
import com.bandyer.core_av.view.OnViewStatusListener;
import com.bandyer.core_av.view.ScaleType;
import com.bandyer.demo_core_av_2.BaseActivity;
import com.bandyer.demo_core_av_2.R;
import com.bandyer.demo_core_av_2.design.bottom_sheet.group_picker.BottomGroupPicker;
import com.bandyer.demo_core_av_2.design.bottom_sheet.group_picker.BottomGroupPickerDelegate;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.viven.imagezoom.ImageZoomHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;

import static android.view.View.GONE;

/**
 * @author kristiyan
 **/

public class SubscriberItem extends AbstractItem<SubscriberItem, SubscriberItem.ViewHolder> {

    private Subscriber subscriber;

    public SubscriberItem(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    @Override
    public long getIdentifier() {
        return subscriber.getId().hashCode();
    }

    @Override
    public int getType() {
        return R.id.subscriber_item;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.item_subscriber;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    /**
     * our ViewHolder
     */
    public static class ViewHolder extends ButterKnifeViewHolder<SubscriberItem> {

        @BindView(R.id.preview)
        BandyerView preview;

        @BindView(R.id.stream_id)
        TextView streamId;

        @BindView(R.id.audioButton)
        AppCompatImageButton audioButton;

        @BindView(R.id.videoButton)
        AppCompatImageButton videoButton;

        @BindView(R.id.subscription_options)
        GridLayout subscription_options;

        @BindView(R.id.changeQualityButton)
        AppCompatImageButton changeQualityButton;

        ViewHolder(View itemView) {
            super(itemView);
            ImageZoomHelper.setViewZoomable(preview);
            preview.bringToFront(true);
        }

        ScaleType scaleType = ScaleType.BALANCED;
        boolean audioMuted = false;
        boolean videoMuted = false;
        Snackbar snackbar;

        VideoResolutionQuality videoResolutionQuality = VideoResolutionQuality.AUTO;
        VideoFpsQuality videoFpsQuality = VideoFpsQuality.AUTO;

        @Override
        public void bindView(@NonNull final SubscriberItem item, @NonNull List<Object> payloads) {
            item.subscriber.setView(preview, new OnStreamListener() {
                @Override
                public void onReadyToPlay(@NonNull Stream stream) {
                    updateAudioVideoButton(stream.getHasVideo(),
                            stream.getHasAudio(),
                            stream.isAudioMuted(),
                            stream.isVideoMuted());
                    preview.play(stream);
                }
            });

            preview.setViewListener(new OnViewStatusListener() {
                @Override
                public void onFirstFrameRendered() {
                    Log.e("SubView", "frameRendered");
                }

                @Override
                public void onViewSizeChanged(int width, int height, int rotationDegree) {
                    Log.e("SubView", "w " + width + " h " + height + "r " + rotationDegree);
                }
            });

            streamId.setText(item.subscriber.getStream().getStreamId());
            streamId.setSelected(true);

            changeQualityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onChangeQuality((BaseActivity) v.getContext(), item);
                }
            });
        }

        @Override
        public void unbindView(@NonNull SubscriberItem item) {
            item.subscriber.releaseView();

            audioMuted = false;
            videoMuted = false;
            scaleType = ScaleType.BALANCED;

            streamId.setText("");
            videoButton.setVisibility(View.VISIBLE);
            audioButton.setVisibility(View.VISIBLE);

            if (snackbar != null) {
                snackbar.dismiss();
            }
        }

        void updateAudioVideoButton(Boolean previewHasVideo, Boolean previewHasAudio, Boolean audioMuted, Boolean videoMuted) {
            this.audioMuted = audioMuted;
            this.videoMuted = videoMuted;

            if (!previewHasVideo)
                videoButton.setVisibility(GONE);

            if (!previewHasAudio)
                audioButton.setVisibility(GONE);

            audioButton.setImageResource(audioMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
            videoButton.setImageResource(videoMuted ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
        }

        void onChangeQuality(BaseActivity activity, final SubscriberItem item) {

            final BottomGroupPicker<VideoQuality> qualityPicker = new BottomGroupPicker<>(activity, activity.getString(R.string.video_quality_picker_title), 2);

            List<VideoQuality> fps = new ArrayList<VideoQuality>(Arrays.asList(VideoFpsQuality.values()));
            final List<VideoQuality> resolutionQualities = new ArrayList<VideoQuality>(Arrays.asList(VideoResolutionQuality.values()));

            qualityPicker
                    .addGroupItems(activity.getString(R.string.fps_title), fps, fps.indexOf(videoFpsQuality))
                    .addGroupItems(activity.getString(R.string.resolution_title), resolutionQualities, resolutionQualities.indexOf(videoResolutionQuality))
                    .setDelegate(new BottomGroupPickerDelegate<VideoQuality>() {
                        @Override
                        public void onClicked(List<VideoQuality> itemsSelected) {

                            int pos = itemsSelected.get(0) instanceof VideoResolutionQuality ? 0 : 1;
                            int fpos = itemsSelected.get(0) instanceof VideoFpsQuality ? 0 : 1;

                            videoResolutionQuality = (VideoResolutionQuality) itemsSelected.get(pos);
                            videoFpsQuality = (VideoFpsQuality) itemsSelected.get(fpos);

                            item.subscriber.setQuality(videoResolutionQuality, videoFpsQuality);
                        }
                    }).show();
        }

        @OnClick(R.id.audioButton)
        void onAudioToggle(AppCompatImageButton view) {
            audioMuted = !audioMuted;
            view.setImageResource(audioMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
            preview.audioEnabled(!audioMuted);
        }

        @OnClick(R.id.videoButton)
        void onVideoToggle(AppCompatImageButton view) {
            videoMuted = !videoMuted;
            view.setImageResource(videoMuted ? R.drawable.ic_videocam_off : R.drawable.ic_videocam);
            preview.videoEnabled(!videoMuted);
        }

        @OnClick(R.id.changeScaleTypeButton)
        void onChangeScaleType(AppCompatImageButton view) {
            ScaleType changed;
            switch (scaleType) {
                case BALANCED:
                    changed = ScaleType.FIT;
                    break;
                case FIT:
                    changed = ScaleType.FILL;
                    break;
                case FILL:
                    changed = ScaleType.BALANCED;
                    break;
                default:
                    changed = ScaleType.BALANCED;
                    break;
            }
            scaleType = changed;
            preview.setScaleType(scaleType);
            snackbar = Snackbar.make(view, "Scale Type changed to: " + changed.name(), Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

    }

    public static class SubscriberItemClickListener extends ClickEventHook<SubscriberItem> {

        @Nullable
        @Override
        public View onBind(RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof SubscriberItem.ViewHolder)
                return ((ViewHolder) viewHolder).itemView;
            return null;
        }

        @Override
        public void onClick(@NonNull View v, int position, @NonNull FastAdapter<SubscriberItem> fastAdapter, @NonNull SubscriberItem item) {
            GridLayout sub_options = v.findViewById(R.id.subscription_options);
            sub_options.setVisibility(sub_options.getVisibility() == View.VISIBLE ? GONE : View.VISIBLE);
        }
    }
}