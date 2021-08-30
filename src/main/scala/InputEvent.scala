package lambdatest

sealed trait InputEvent

case object GetListEvent extends InputEvent

case class AddTaskEvent(desc: String) extends InputEvent

case class ToggleTaskEvent(id: String) extends InputEvent

case class RemoveTaskEvent(id: String) extends InputEvent

case object RemoveDoneEvent extends InputEvent
