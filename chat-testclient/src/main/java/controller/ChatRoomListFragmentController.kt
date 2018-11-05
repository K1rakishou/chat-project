package controller

import ChatApp
import core.Constants
import core.ResponseInfo
import core.ResponseType
import core.Status
import core.exception.ResponseDeserializationException
import core.packet.SearchChatRoomPacket
import core.response.SearchChatRoomResponsePayload
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import manager.NetworkManager
import store.SearchChatRoomsStore
import ui.chat_main_window.ChatRoomListFragment
import utils.ThreadChecker

class ChatRoomListFragmentController : BaseController<ChatRoomListFragment>() {
  private val networkManager: NetworkManager by lazy { ChatApp.networkManager }
  private val searchChatRoomsStore: SearchChatRoomsStore by lazy { ChatApp.searchChatRoomsStore }

  override fun createController(viewParam: ChatRoomListFragment) {
    super.createController(viewParam)

    startListeningToPackets()
  }

  override fun destroyController() {
    super.destroyController()
  }

  fun sendSearchRequest(chatRoomNameToSearch: String) {
    if (!networkManager.isConnected) {
      println("Not connected to the server")
      return
    }

    if (chatRoomNameToSearch.isBlank()) {
      println("chatRoomNameToSearch is blank")
      return
    }

    if (chatRoomNameToSearch.length > Constants.maxChatRoomNameLength) {
      println("chatRoomNameToSearch length (${chatRoomNameToSearch.length}) exceeds Constants.maxChatRoomNameLength (${Constants.maxChatRoomNameLength})")
      return
    }

    networkManager.sendPacket(SearchChatRoomPacket(chatRoomNameToSearch))
  }

  private fun startListeningToPackets() {
    compositeDisposable += networkManager.responsesFlowable
      .filter { networkManager.isConnected }
      .subscribe(this::handleIncomingResponses, { it.printStackTrace() })

    compositeDisposable += networkManager.connectionStateObservable
      .filter { connectionState -> connectionState != NetworkManager.ConnectionState.Connected }
      .subscribeBy(onNext = { connectionState ->
        when (connectionState) {
          is NetworkManager.ConnectionState.Uninitialized -> {
            //Default state
          }
          is NetworkManager.ConnectionState.Connecting -> {
          }
          is NetworkManager.ConnectionState.Disconnected -> {
          }
          is NetworkManager.ConnectionState.ErrorWhileTryingToConnect -> {
          }
          is NetworkManager.ConnectionState.Connected -> {
          }
          is NetworkManager.ConnectionState.Reconnected -> {
          }
        }
      })
  }

  private fun handleIncomingResponses(responseInfo: ResponseInfo) {
    doOnBg {
      when (responseInfo.responseType) {
        ResponseType.SearchChatRoomResponseType -> handleSearchChatRoomResponse(responseInfo)
        else -> {
          //Do nothing
        }
      }
    }
  }

  private fun handleSearchChatRoomResponse(responseInfo: ResponseInfo) {
    println("SearchChatRoomResponseType response received")
    ThreadChecker.throwIfOnMainThread()

    val response = try {
      SearchChatRoomResponsePayload.fromByteSink(responseInfo.byteSink)
    } catch (error: ResponseDeserializationException) {
      error.printStackTrace()
      return
    }

    if (response.status != Status.Ok) {
      println("Response contains non-ok status: ${response.status}")
      return
    }

    doOnUI {
      searchChatRoomsStore.reloadSearchChatRoomList(response.foundChatRooms)
    }
  }
}