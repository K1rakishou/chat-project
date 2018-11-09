package ui.chat_main_window

import ChatApp
import controller.ChatRoomListFragmentController
import core.Constants
import utils.helper.DebouncedSearchHelper
import events.ChatMainWindowEvents
import events.ChatRoomListFragmentEvents
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
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
import model.chat_room_list.PublicChatRoomItem
import model.chat_room_list.SearchChatRoomItem
import org.fxmisc.flowless.VirtualizedScrollPane
import store.ChatRoomsStore
import store.SearchChatRoomsStore
import tornadofx.*
import ui.base.BaseFragment
import ui.widgets.VirtualListView

class ChatRoomListFragment : BaseFragment() {
  private lateinit var searchTextInput: TextField

  private val controller: ChatRoomListFragmentController by inject()
  private val chatRoomsStore: ChatRoomsStore by lazy { ChatApp.chatRoomsStore }
  private val searchChatRoomsStore: SearchChatRoomsStore by lazy { ChatApp.searchChatRoomsStore }

  private var lastSelectedChatRoomName: String? = null
  private val rightMargin = 16.0

  private val debouncedSearch = DebouncedSearchHelper()
  private var currentListState = ListState.NormalState
  private val chatRoomsListProperty = FXCollections.observableArrayList<BaseChatRoomListItem>()
  private val chatRoomsListPropertyListener = ListConversionListener<BaseChatRoomListItem, BaseChatRoomListItem>(chatRoomsListProperty) { it }
  private val chatMainWindowSize = params[ChatMainWindow.CHAT_ROOM_LIST_VIEW_SIZE] as ChatMainWindow.ChatRoomViewSizeParams

  init {
    //TODO: should probably add delay here
    subscribe<ChatRoomListFragmentEvents.SelectItem> { event ->
      selectItem(event.key)
    }.autoUnsubscribe()
    subscribe<ChatRoomListFragmentEvents.ClearSelection> {
      lastSelectedChatRoomName = null
      virtualListView.clearSelection()
    }.autoUnsubscribe()
    subscribe<ChatRoomListFragmentEvents.ClearSearchInput> {
      searchTextInput.clear()
      selectItem(lastSelectedChatRoomName)
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
      is PublicChatRoomItem -> createCellPublicChatRoomItem(chatMainWindowSize.widthProperty, item)
      is NoRoomsNotificationItem -> createCellNoRoomsNotificationItem(chatMainWindowSize.widthProperty, item.message)
      is SearchChatRoomItem ->  createCellSearchChatRoomItem(chatMainWindowSize.widthProperty, item)
      else -> throw RuntimeException("Not implemented for ${item::class}")
    }
  }, { item ->
    item.roomName
  }, { selectedItem ->
    onItemSelected(selectedItem)
  })

  override val root = vbox {
    searchTextInput = textfield {
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
      background = Background(BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, Insets.EMPTY))
      prefHeightProperty().bind(chatMainWindowSize.heightProperty)
    }))
  }

  private fun selectItem(key: String?) {
    if (key != null) {
      lastSelectedChatRoomName = key
      virtualListView.selectItemByKey(key)
    } else {
      virtualListView.selectItemByKey(lastSelectedChatRoomName)
    }
  }

  private fun performSearch(chatRoomName: String) {
    doOnUI {
      val newListState = ListState.fromText(chatRoomName)
      if (currentListState == newListState) {
        return@doOnUI
      }

      currentListState = newListState
      reloadChatRoomsList(currentListState)

      if (newListState != ListState.NotEnoughSymbols) {
        controller.sendSearchRequest(chatRoomName)
      }
    }
  }

  private fun reloadChatRoomsList(listState: ListState) {
    val (oldSearchChatRoomsProperty, newSearchChatRoomsProperty) = when (listState) {
      ListState.SearchState -> chatRoomsStore.publicChatRoomList to searchChatRoomsStore.searchChatRoomList
      ListState.NormalState -> searchChatRoomsStore.searchChatRoomList to chatRoomsStore.publicChatRoomList
      ListState.NotEnoughSymbols -> return
    }

    oldSearchChatRoomsProperty.removeListener(chatRoomsListPropertyListener)

    chatRoomsListProperty.clear()
    chatRoomsListProperty.addAll(newSearchChatRoomsProperty)

    newSearchChatRoomsProperty.addListener(chatRoomsListPropertyListener)
  }

  private fun onItemSelected(item: BaseChatRoomListItem) {
    when (item) {
      is PublicChatRoomItem -> {
        val isMyUserAdded = chatRoomsStore.getChatRoomByName(item.roomName)?.isMyUserAdded() ?: false
        if (isMyUserAdded) {
          if (lastSelectedChatRoomName == null || lastSelectedChatRoomName != item.roomName) {
            lastSelectedChatRoomName = item.roomName
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
        alignment = Pos.CENTER_LEFT
        minWidth = 8.0
      }
    }
  }

  //TODO: extract to it's own class?
  private fun createCellPublicChatRoomItem(widthProperty: ReadOnlyDoubleProperty, item: PublicChatRoomItem): HBox {
    return hbox {
      prefHeight = 64.0
      maxHeight = 64.0
      paddingAll = 2.0
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty)
      maxWidthProperty().bind(widthProperty)

      imageview(item.imageUrl) {
        minHeight = 60.0
        fitHeight = 60.0
        isPreserveRatio = true
        isSmooth = true
        alignment = Pos.CENTER_LEFT
        paddingRight = 8.0
      }
      label { minWidth = 8.0 }
      vbox {
        label(item.roomName) {
          textOverrun = OverrunStyle.ELLIPSIS
        }
        label {
          //TODO: remove null assert
          textProperty().bind(chatRoomsStore.getChatRoomByName(item.roomName)!!.lastMessageProperty)

          textOverrun = OverrunStyle.ELLIPSIS
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
      cursor = Cursor.HAND

      prefWidthProperty().bind(widthProperty)
      maxWidthProperty().bind(widthProperty)

      imageview(item.imageUrl) {
        minHeight = 60.0
        fitHeight = 60.0
        isPreserveRatio = true
        isSmooth = true
        alignment = Pos.CENTER_LEFT
        paddingRight = 8.0
      }
      label { minWidth = 8.0 }
      vbox {
        label(item.roomName) {
          textOverrun = OverrunStyle.ELLIPSIS
        }
      }
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