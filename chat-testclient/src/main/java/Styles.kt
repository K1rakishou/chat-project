import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*

class Styles : Stylesheet() {
  init {
    initConnectionWindowStyle()
    initLoadingWindowStyle()
    initChatMainWindowStyle()
    initChatRoomListFragmentStyle()
    initPositiveButtonStyle()
    initTextInputStyle()

    splitpane {
      backgroundColor += bgColorDark

      splitPaneDivider {
        backgroundColor += accntColorDark
        prefWidth = 4.px
      }
    }

    scrollView {
      backgroundColor += bgColorDark

      incrementArrow {
        backgroundColor += accntColorBright
      }

      incrementArrowButton {
        backgroundColor += accntColorBright
      }

      decrementArrow {
        backgroundColor += accntColorBright
      }

      decrementArrowButton {
        backgroundColor += accntColorBright
      }

      track {
        prefWidth = 2.px
        backgroundColor += accntColorDark
      }

      thumb {
        prefWidth = 8.px
        backgroundColor += accntColorBright
      }
    }

    chatRoomTextField {
      fontSize = 18.px
      textFill = txtColor
    }
    chatRoomViewEmptyLabel {
      fontSize = 24.px
      textFill = txtColor
    }

    systemMessage {
      fontSize = 18.px
      fontWeight = FontWeight.BOLD
      textFill = c(0, 187, 205)
    }
    senderName {
      fontSize = 18.px
      fontWeight = FontWeight.BOLD
      textFill = c(102, 205, 0)
    }
    receiverName {
      fontSize = 18.px
      fontWeight = FontWeight.BOLD
      textFill = c(244, 0, 122)
    }

    myTextChatMessage {
      fontSize = 16.px
      textFill = c(172, 172, 172)
    }
    myTextChatMessageAcceptedByServer {
      fontSize = 16.px
      textFill = txtColor
    }
    systemTextChatMessage {
      fontSize = 16.px
      textFill = txtColor
    }
    foreignTextChatMessage {
      fontSize = 16.px
      textFill = txtColor
    }
  }

  private fun initTextInputStyle() {
    textInput {
      backgroundColor += bgColorDark
      borderColor += box(accntColorDark)
      textFill = txtColor
      accentColor = accntColorDark

      and(focused) {
        backgroundColor += bgColorBright
      }

      and(hover) {
        borderColor += box(accntColorBright)
      }
    }
  }

  private fun initPositiveButtonStyle() {
    positiveButton {
      backgroundColor += accntColorDark
      borderColor += box(txtColor)
      textFill = txtColor

      and(hover) {
        backgroundColor += accntColorBright
        borderColor += box(Color.WHITE)
      }
    }
  }

  private fun initChatRoomListFragmentStyle() {
    chatRoomListFragment {
      backgroundColor += bgColorDark
    }
  }

  private fun initChatMainWindowStyle() {
    chatMainWindow {
      backgroundColor += bgColorDark
      borderColor += box(bgColorDark)

      menuBar {
        backgroundColor += bgColorDark

        menu {
          label {
            textFill = txtColor
          }

          and(hover) {
            backgroundColor += bgColorBright
          }

          menuItem {
            backgroundColor += bgColorDark

            and(hover) {
              backgroundColor += bgColorBright
            }
          }
        }
      }
    }
  }

  private fun initLoadingWindowStyle() {
    loadingWindow {
      backgroundColor += bgColorDark

      progressIndicator {
        progressColor = accntColorDark
      }
    }
  }

  private fun initConnectionWindowStyle() {
    connectionWindow {
      backgroundColor += bgColorDark

      fieldset {
        field {
          label {
            textFill = txtColor
          }
        }
      }
    }
  }

  companion object {
    val bgColorDark = c(45, 46, 49)
    val bgColorBright = c(75, 76, 79)
    val txtColor = c(196, 198, 197)
    val accntColorDark = c(0, 108, 108)
    val accntColorBright = c(0, 148, 148)
    val complementaryDark = c(128, 0, 0)
    val complementaryBright = c(168, 0, 0)
    val analogousDark = c(0, 32, 128)
    val analogousBright = c(0, 64, 160)

    //connection window
    val connectionWindow by cssclass()

    //loading window
    val loadingWindow by cssclass()

    //chat main window
    val chatMainWindow by cssclass()

    //chat room list fragment
    val chatRoomListFragment by cssclass()

    val splitpane by cssclass()
    val scrollView by cssclass()

    val chatRoomTextField by cssclass()
    val chatRoomViewEmptyLabel by cssclass()

    val systemMessage by cssclass()
    val senderName by cssclass()
    val receiverName by cssclass()

    val myTextChatMessage by cssclass()
    val myTextChatMessageAcceptedByServer by cssclass()
    val systemTextChatMessage by cssclass()
    val foreignTextChatMessage by cssclass()

    val positiveButton by cssclass()
    val textInput by cssclass()
  }
}