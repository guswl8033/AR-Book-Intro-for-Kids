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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})

public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // Models of the 4 corners.  We use completable futures here to simplify
  // the error handling and asynchronous loading.  The loading is started with the
  // first construction of an instance, and then used when the image is set.
  private static CompletableFuture<ModelRenderable> ulCorner;
  private static CompletableFuture<ModelRenderable> urCorner;
  private static CompletableFuture<ModelRenderable> lrCorner;
  private static CompletableFuture<ModelRenderable> llCorner;

  // Add a member variable to hold the maze model.
  private Node mazeNode;

  // Add a variable called mazeRenderable for use with loading
  private CompletableFuture<ModelRenderable> mazeRenderable0;
  private CompletableFuture<ModelRenderable> mazeRenderable1;
    private CompletableFuture<ModelRenderable> mazeRenderable2;

  // sfb파일을 renderable로 build함
  public AugmentedImageNode(Context context) {
      mazeRenderable0 =
              ModelRenderable.builder()
                      .setSource(context, Uri.parse(((FirstActivity) FirstActivity.context_first).BookName + "0.sfb"))
                      .build();
      mazeRenderable1=
              ModelRenderable.builder()
                      .setSource(context, Uri.parse(((FirstActivity) FirstActivity.context_first).BookName + "1.sfb"))
                      .build();
      mazeRenderable2=
              ModelRenderable.builder()
                      .setSource(context, Uri.parse(((FirstActivity) FirstActivity.context_first).BookName + "2.sfb"))
                      .build();

  }
//
  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  private float maze_scale = 0.0f;
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})

  public void setImage(AugmentedImage image) {
    this.image = image; //anchor이 될 image, 본 프로젝트에서는 책의 그림

    // Initialize mazeNode and set its parents and the Renderable.
    // If any of the models are not loaded, process this function
    // until they all are loaded.

      if (!mazeRenderable0.isDone()) {
          CompletableFuture.allOf(mazeRenderable0)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
          return;
      }
      if (!mazeRenderable1.isDone()) {
          CompletableFuture.allOf(mazeRenderable1)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
          return;
      }
      if (!mazeRenderable2.isDone()) {
          CompletableFuture.allOf(mazeRenderable2)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
          return;
      }

      // image의 중심을 anchor로 만들어줌
      setAnchor(image.createAnchor(image.getCenterPose()));

      mazeNode = new Node();
      mazeNode.setParent(this);
      mazeNode.setRenderable(mazeRenderable0.getNow(null));
      //node의 renderable을 설정해줌

      //activity의 화살표 버튼이 눌릴때마다 node의 renderable을 업데이트 해준다.
      //각각의 node위치를 vector를 이용해 설정해준다. v:x-좌우, v1:y-위아래, v2:z-앞뒤 방향
      switch(((AugmentedImageActivity) AugmentedImageActivity.context_main).butNum){
          case 1:
              mazeNode.setRenderable(mazeRenderable1.getNow(null));
              mazeNode.setLocalPosition(new Vector3(0.05f,0, 0));
              break;
          case 2:
              mazeNode.setRenderable(mazeRenderable2.getNow(null));
              mazeNode.setLocalPosition(new Vector3(0.03f,0.1f, 0));
              break;
      }


    // Make sure the longest edge fits inside the image.
    final float maze_edge_size = 492.65f;
    final float max_image_edge = Math.max(image.getExtentX(), image.getExtentZ());
    maze_scale = max_image_edge / maze_edge_size;


   }

}


