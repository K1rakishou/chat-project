package repository.mapper

import core.model.drainable.chat_message.BaseChatMessage
import core.model.drainable.chat_message.TextChatMessage
import repository.model.BaseChatMessageData
import repository.model.TextChatMessageData
import java.lang.IllegalArgumentException

object BaseChatMessageMapper {

  object FromBaseChatMessage {
    fun toBaseChatMessageData(clientId: String, serverMessageId: Int, baseChatMessage: BaseChatMessage): BaseChatMessageData {
      return when (baseChatMessage) {
        is TextChatMessage -> TextChatMessageData(
          clientId,
          serverMessageId,
          baseChatMessage.clientMessageId,
          baseChatMessage.senderName,
          baseChatMessage.message
        )
        else -> throw IllegalArgumentException("Not implemented for ${baseChatMessage::class}")
      }
    }
  }

  object FromBaseChatMessageData {
    fun toBaseChatMessage(clientId: String, baseChatMessageData: BaseChatMessageData): BaseChatMessage {
      val isMyMessage = clientId == baseChatMessageData.clientId

      return when (baseChatMessageData) {
        is TextChatMessageData -> TextChatMessage(
          isMyMessage,
          baseChatMessageData.serverMessageId,
          baseChatMessageData.clientMessageId,
          baseChatMessageData.senderName,
          baseChatMessageData.message
        )
        else -> throw IllegalArgumentException("Not implemented for ${baseChatMessageData::class}")
      }
    }

    fun toBaseChatMessageList(clientId: String, baseChatMessageDataList: List<BaseChatMessageData>): List<BaseChatMessage> {
      return baseChatMessageDataList.map { toBaseChatMessage(clientId, it) }
    }
  }

}