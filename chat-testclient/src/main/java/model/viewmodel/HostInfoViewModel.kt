package model.viewmodel

import tornadofx.ViewModel

class HostInfoViewModel(
  val host: String,
  val port: String
) : ViewModel() {

  fun isEmpty() = host.isEmpty() || port.isEmpty()
}