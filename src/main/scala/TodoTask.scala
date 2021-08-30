package lambdatest

import java.util.concurrent.ThreadLocalRandom

case class TodoTask(desc: String, done: Boolean = false, id: String = ThreadLocalRandom.current().nextLong().toHexString)
