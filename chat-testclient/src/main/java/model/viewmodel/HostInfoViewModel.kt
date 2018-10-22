package model.viewmodel

import tornadofx.ViewModel

class HostInfoViewModel(
  val ip: String,
  val port: String
) : ViewModel() {

  fun isEmpty() = ip.isEmpty() || port.isEmpty()
}