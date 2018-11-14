package ui.chat_main_window.full_image_view_window

import ChatApp
import builders.TransformationBuilder
import core.CachingImageLoader
import core.SaveStrategy
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.image.ImageView
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Screen
import kotlinx.coroutines.future.await
import tornadofx.hbox
import tornadofx.hgrow
import tornadofx.imageview
import tornadofx.vgrow
import ui.base.BaseFragment

class FullImageViewWindow : BaseFragment() {
  private var imageView: ImageView? = null
  private val scaleDelta = 1.2

  private val imageLoader: CachingImageLoader by lazy { ChatApp.imageLoader }

  private val url: String = params["url"] as String
  private val x: Double = params["x"] as Double
  private val y: Double = params["y"] as Double
  private val width: Double = params["width"] as Double
  private val height: Double = params["height"] as Double

  override fun onDock() {
    super.onDock()

    if (!setupWindow()) {
      return
    }

    loadImage()
  }

  private fun setupWindow(): Boolean {
    if (currentStage == null) {
      println("Current stage is null")
      return false
    }

    val screen = Screen.getScreensForRectangle(x, y, width, height).firstOrNull()
    if (screen == null) {
      println("Could not get current screen")
      return false
    }

    val bounds = screen.visualBounds

    currentStage!!.x = bounds.minX
    currentStage!!.y = bounds.minY
    currentStage!!.width = bounds.width
    currentStage!!.height = bounds.height

    return true
  }

  private fun loadImage() {
    doOnBg {
      val image = imageLoader.newRequest()
        .load(url)
        .transformations(TransformationBuilder().noTransformations())
        .saveStrategy(SaveStrategy.SaveOriginalImage)
        .getAsync()
        .await()

      if (image == null) {
        closeWindow()
        return@doOnBg
      }

      doOnUI {
        root.apply {
          imageView = imageview {
            alignment = Pos.CENTER

            fitWidth = image.width
            fitHeight = image.height

            imageProperty().set(image)
          }
        }
      }
    }
  }

  override val root = hbox {
    cursor = Cursor.HAND
    vgrow = Priority.ALWAYS
    hgrow = Priority.ALWAYS

    background = Background(BackgroundFill(Color(0.0, 0.0, 0.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY))

    setOnMouseClicked {
      closeWindow()
    }
    setOnScroll { event ->
      if (event.deltaY == 0.0) {
        return@setOnScroll
      }

      val factor = if (event.deltaY > 0) {
        scaleDelta
      } else {
        1 / scaleDelta
      }

      imageView?.let {
        it.scaleX = it.scaleX * factor
        it.scaleY = it.scaleY * factor
      }
    }
  }

  private fun closeWindow() {
    currentStage!!.close()
  }
}