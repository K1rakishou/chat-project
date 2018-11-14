package ui.chat_main_window

import ChatApp
import builders.CircleCropParametersBuilder
import builders.TransformationBuilder
import controller.ChatRoomListFragmentController
import core.CachingImageLoader
import core.Constants
import core.SaveStrategy
import utils.helper.DebouncedSearchHelper
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.OverrunStyle
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Paint
import model.chat_room_list.BaseChatRoomListItem
import model.chat_room_list.NoRoomsNotificationItem
import model.chat_room_list.ChatRoomItem
import model.chat_room_list.SearchChatRoomItem
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import store.SearchChatRoomsStore
import store.SelectedRoomStore
import tornadofx.*
import ui.base.BaseFragment
import ui.widgets.VirtualListView
import utils.UiValidators
import java.awt.Color
import java.lang.IllegalStateException

class ChatRoomListFragment : BaseFragment() {
  private lateinit var searchTextInput: TextField

  private val controller: ChatRoomListFragmentController by inject()
  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val selectedRoomStore: SelectedRoomStore by lazy { ChatApp.selectedRoomStore }
  private val searchChatRoomsStore: SearchChatRoomsStore by lazy { ChatApp.searchChatRoomsStore }
  private val imageLoader: CachingImageLoader by lazy { ChatApp.imageLoader }

  private val rightMargin = 16.0
  private val debouncedSearch = DebouncedSearchHelper()
  private val chatRoomsListProperty = FXCollections.observableArrayList<BaseChatRoomListItem>()
  private val chatRoomsListPropertyListener = ListConversionListener<BaseChatRoomListItem, BaseChatRoomListItem>(chatRoomsListProperty) { it }
  private val chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_LIST_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  private var currentListState = ListState.NormalState

  init {
    selectedRoomStore.getSelectedRoomProperty().addListener { _, _, selectedRoomName ->
      selectItem(selectedRoomName)
    }

    subscribe<ChatRoomListFragmentEvents.ClearSelection> {
      virtualListView.clearSelection()
      clearSelection()
    }.autoUnsubscribe()
    subscribe<ChatRoomListFragmentEvents.ClearSearchInput> {
      searchTextInput.clear()
      clearSelection()
    }.autoUnsubscribe()
  }

  override fun onDock() {
    controller.createController(this)

    debouncedSearch.start(this::performSearch)
    reloadChatRoomsList(ListState.NormalState)
  }

  override fun onUndock() {
    debouncedSearch.stop()

    controller.destroyController()
  }

  private val virtualListView = VirtualListView(chatRoomsListProperty, { item ->
    return@VirtualListView when (item) {
      is ChatRoomItem -> createCellChatRoomItem(chatMainWindowSize.widthProperty, item)
      is NoRoomsNotificationItem -> createCellNoRoomsNotificationItem(chatMainWindowSize.widthProperty, item.message)
      is SearchChatRoomItem -> createCellSearchChatRoomItem(chatMainWindowSize.widthProperty, item)
      else -> throw RuntimeException("Not implemented for ${item::class}")
    }
  }, { item ->
    item.roomName
  }, { selectedItem ->
    onItemSelected(selectedItem)
  })

  override val root = vbox {
    addClass(Styles.chatRoomListFragment)

    searchTextInput = textfield {
      addClass(Styles.textInput)

      val context = ValidationContext()
      context.addValidator(this@textfield, this.textProperty()) { searchString ->
        UiValidators.validateSearchString(this, searchString)
      }

      promptText = "Search for a chat room"

      setOnKeyReleased { event ->
        if (event.code == KeyCode.ESCAPE) {
          clear()
        }
      }

      textProperty().addListener { _, _, text ->
        debouncedSearch.process(text)
      }
    }

    add(VirtualizedScrollPane(virtualListView.getVirtualFlow().apply {
      background = Background(BackgroundFill(Styles.bgColorDark, CornerRadii.EMPTY, Insets.EMPTY))
      prefHeightProperty().bind(chatMainWindowSize.heightProperty)
    }))
  }

  private fun performSearch(chatRoomName: String) {
    doOnUI {
      val newListState = ListState.fromText(chatRoomName)
      if (currentListState == newListState) {
        return@doOnUI
      }

      currentListState = newListState
      reloadChatRoomsList(currentListState)

      when (newListState) {
        ListState.SearchState -> controller.sendSearchRequest(chatRoomName)
        ListState.NormalState -> {
          val roomName = selectedRoomStore.getSelectedOrPrevSelected()
          if (roomName != null) {
            selectItem(roomName)
          }
        }
        ListState.NotEnoughSymbols -> {
          //do nothing
        }
      }
    }
  }

  //if roomName is null - try to select an item by lastSelectedRoom
  private fun selectItem(roomName: String) {
    virtualListView.selectItemByKey(roomName)
  }

  private fun clearSelection() {
    virtualListView.clearSelection()
  }

  private fun reloadChatRoomsList(listState: ListState) {
    val (oldSearchChatRoomsProperty, newSearchChatRoomsProperty) = when (listState) {
      ListState.SearchState -> chatRoomsStore.chatRoomList to searchChatRoomsStore.searchChatRoomList
      ListState.NormalState -> searchChatRoomsStore.searchChatRoomList to chatRoomsStore.chatRoomList
      ListState.NotEnoughSymbols -> {
        //do nothing
        return
      }
    }

    oldSearchChatRoomsProperty.removeListener(chatRoomsListPropertyListener)

    chatRoomsListProperty.clear()
    chatRoomsListProperty.addAll(newSearchChatRoomsProperty)

    newSearchChatRoomsProperty.addListener(chatRoomsListPropertyListener)
  }

  private fun onItemSelected(item: BaseChatRoomListItem) {
    when (item) {
      is ChatRoomItem -> {
        val isMyUserAdded = chatRoomsStore.getChatRoomByName(item.roomName)?.isMyUserAdded() ?: false
        if (isMyUserAdded) {
          val lastSelectedChatRoomName = selectedRoomStore.getSelectedRoom()
          if (lastSelectedChatRoomName == null || lastSelectedChatRoomName != item.roomName) {
            fire(ChatMainWindowEvents.ShowChatRoomViewEvent(item.roomName))
          }
        }
      }
      is NoRoomsNotificationItem -> {
        if (item.notificationType == NoRoomsNotificationItem.NotificationType.JoinedRoomsNotificationType) {
          fire(ChatMainWindowEvents.ShowCreateChatRoomDialogEvent)
        }
      }
      is SearchChatRoomItem -> {
        fire(ChatMainWindowEvents.ShowJoinChatRoomDialogEvent(item.roomName))
      }
      else -> throw RuntimeException("Not implemented for ${item::class}")
    }
  }

  //TODO: extract to it's own class?
  private fun createCellNoRoomsNotificationItem(widthProperty: ReadOnlyDoubleProperty, message: String): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty - rightMargin)
      maxWidthProperty().bind(widthProperty - rightMargin)

      label(message) {
        textFill = Styles.txtColor
        alignment = Pos.CENTER_LEFT
        minWidth = 8.0
      }
    }
  }

  //TODO: extract to it's own class?
  private fun createCellChatRoomItem(widthProperty: ReadOnlyDoubleProperty, item: ChatRoomItem): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      alignment = Pos.CENTER_LEFT
      paddingRight = 8.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty)
      maxWidthProperty().bind(widthProperty)

      createImageView(item)
      label { minWidth = 8.0 }
      vbox {
        label(item.roomName) {
          textFill = Styles.txtColor
          textOverrun = OverrunStyle.ELLIPSIS
        }
        label {
          textFill = Styles.txtColor
          textOverrun = OverrunStyle.ELLIPSIS

          //TODO: remove null assert
          textProperty().bind(chatRoomsStore.getChatRoomByName(item.roomName)!!.lastMessageProperty)
        }
      }
    }
  }

  //TODO: extract to it's own class?
  private fun createCellSearchChatRoomItem(widthProperty: ReadOnlyDoubleProperty, item: SearchChatRoomItem): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      alignment = Pos.CENTER_LEFT
      paddingRight = 8.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty)
      maxWidthProperty().bind(widthProperty)

      createImageView(item)
      label { minWidth = 8.0 }
      vbox {
        label(item.roomName) {
          textFill = Styles.txtColor
          textOverrun = OverrunStyle.ELLIPSIS
        }
      }
    }
  }

  private fun Node.createImageView(item: BaseChatRoomListItem) {
    val imageUrl = when (item) {
      is ChatRoomItem -> item.imageUrl
      is SearchChatRoomItem -> item.imageUrl
      else -> throw IllegalStateException("Not supported")
    }

    imageview {
      fitWidth = 60.0
      fitHeight = 60.0
      isPreserveRatio = true
      isSmooth = true

      imageLoader.newRequest()
        .load(imageUrl)
        .transformations(
          TransformationBuilder()
            .centerCrop(this)
            .circleCrop(
              CircleCropParametersBuilder()
                .backgroundColor(Color(0f, 0f, 0f, 0f))
                .stroke(6f, Color.WHITE)
            )
        )
        .saveStrategy(SaveStrategy.SaveTransformedImage)
        .into(this)
    }
  }

  enum class ListState {
    SearchState,
    NormalState,
    NotEnoughSymbols;

    companion object {
      fun fromText(text: String): ListState {
        if (text.isEmpty()) {
          return NormalState
        }

        if (text.length < Constants.minChatRoomSearchLen) {
          return NotEnoughSymbols
        }

        return SearchState
      }
    }
  }
}