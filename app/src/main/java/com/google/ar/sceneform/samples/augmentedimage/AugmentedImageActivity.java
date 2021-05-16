/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class AugmentedImageActivity extends AppCompatActivity {
  /*
  public static Context context_main; // context 변수 선언
  public static final int sub = 1002;
  */
  public static Context context_main;
  private ArFragment arFragment;
  private ImageView fitToScanView;
  Button buttonaft;
  public int butNum=0;

  public static Context context_AR;
  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
  //원래 final이었음
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    context_main = this;//버튼 누를때 값 AugmentedNode에 전송하기 위한 context 지정
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment); //ar송출Fragment(장면)
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    buttonaft = findViewById(R.id.buttonaft);
    buttonaft.setOnClickListener(view -> {
      butNum++;//버튼 클릭하면 butNum 증가
    });
   arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);//업데이트 된 Fragment를 송출
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }
  int numb=0;
  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();//ARFrame Fragment에 부여

    // If there is no frame or ARCore is not tracking yet, just return.
    if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);


    for (AugmentedImage augmentedImage : updatedAugmentedImages) {

      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = "Detected Image " + augmentedImage.getIndex();
          SnackbarHelper.getInstance().showMessage(this, text);
          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
            String text1 = Integer.toString(butNum);
            SnackbarHelper.getInstance().showMessage(this, text1); //butNum
            fitToScanView.setVisibility(View.GONE);
            // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            AugmentedImageNode node = new AugmentedImageNode(this);
            node.setImage(augmentedImage);//augmentedImage의 중심으로 Anchor설정, Node의 Renderable 설정
            augmentedImageMap.replace(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
            //책 이미지가 Anchor로 설정되어있고, Renderable이 설정되어있는 node Fragment(장면) child로 지정
          }
          break;

        case STOPPED:
          augmentedImageMap.remove(augmentedImage);
          break;
      }
    }
  }
}