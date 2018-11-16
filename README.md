# FUSuwangDemo(android)
## 概述
FUSuwangDemo 是集成了 Faceunity 面部跟踪和虚拟道具功能和网宿推流 SDK 的 Demo 。 本文是 FaceUnity SDK 快速对接网宿推流 SDK 的导读说明，关于 FaceUnity SDK 的更多详细说明，请参看 FULiveDemo.

# 快速集成方法
## 添加module
添加faceunity module到工程中，在app dependencies里添加`compile project(':faceunity')`
## 修改代码
在MyApp中初始化nama
```
FURenderer.initFURenderer(this);
```
在MainActivity中初始化，并嵌入滤镜
```
mFURenderer = new FURenderer
        .Builder(this)
        .inputTextureType(0)
        .setOnTrackingStatusChangedListener(new FURenderer.OnTrackingStatusChangedListener() {
            @Override
            public void onTrackingStatusChanged(final int status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_track_text.setVisibility(status > 0 ? View.INVISIBLE : View.VISIBLE);
                    }
                });
            }
        })
        .defaultEffect(EffectEnum.Effect_fengya_ztt_fu.effect())
        .build();
beautyControlView = (BeautyControlView) findViewById(R.id.faceunity_control);
beautyControlView.setOnFaceUnityControlListener(mFURenderer);
filter = new FaceUnityFilter(this, mFURenderer);
filter.setCameraId(mCurrentCameraId);
SPManager.setFilter(filter);
```