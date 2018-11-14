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
        prefWidth = 1.px
      }
    }

    chatRoomTextArea {
      fontSize = 18.px
    }
    chatRoomTextField {
      fontSize = 18.px
    }
    chatRoomViewEmptyLabel {
      fontSize = 24.px
      textFill = txtColor
    }

    systemMessage {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(47, 47, 175)
    }
    senderName {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(175, 47, 47)
    }
    receiverName {
      fontSize = 20.px
      fontWeight = FontWeight.BOLD
      textFill = c(47, 175, 47)
    }

    myTextChatMessage {
      fontSize = 16.px
      textFill = c(172, 172, 172)
    }
    myTextChatMessageAcceptedByServer {
      fontSize = 16.px
      textFill = c(0, 0, 0)
    }
    systemTextChatMessage {
      fontSize = 16.px
      textFill = c(0, 0, 0)
    }
    foreignTextChatMessage {
      fontSize = 16.px
      textFill = c(0, 0, 0)
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
    val accntColorDark = c(0, 128, 128)
    val accntColorBright = c(0, 168, 168)
    val complementaryDark = c(128, 0, 0)
    val complementaryBright = c(168, 0, 0)

    //connection window
    val connectionWindow by cssclass()

    //loading window
    val loadingWindow by cssclass()

    //chat main window
    val chatMainWindow by cssclass()

    //chat room list fragment
    val chatRoomListFragment by cssclass()

    val splitpane by cssclass()

    val chatRoomTextArea by cssclass()
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