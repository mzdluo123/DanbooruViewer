package win.rainchan.win


import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock

class SplitInputStream(private val length: Int) : InputStream() {

    private val buffer = ByteArray(length)
    private var cursor = 0
    private var availableIndex = 0
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val patches = arrayListOf<IntArray>()


    override fun read(): Int {
        lock.lock()
        try {
            while (true) {
                if (cursor + 1 == length) {  // 读到头了
                    return -1
                }
                if (cursor + 1 <= availableIndex) {   // 还有数据可读
                    cursor += 1
                    return buffer[cursor - 1].toInt()
                }
                condition.await()//等待资源
            }
        } finally {
            lock.unlock()
        }

    }

    fun putData(array: ByteArray, start: Int) {
        lock.lock()
        try {
            if (array.isEmpty()) {
                return
            }
            if (array.size + start >= length) {   // 长度超出总长
                throw java.lang.IllegalArgumentException("长度超限")
            }
            System.arraycopy(array, 0, buffer, start, array.size)
            val patchPos = intArrayOf(start, start + array.size)
            updatePatches(patchPos)
            if (cursor != availableIndex) { // 有新的可读连续数据或者是读到头才能解除读的锁定
                condition.signal()
            }
        } finally {

            lock.unlock()
        }
    }

    private fun mergePatches() {
        var shouldloop = true
        while (shouldloop) {
            loop@ for ((ii, iv) in patches.withIndex()) {
                for ((ji, jv) in patches.withIndex()) {
                    if (ii == ji) {  // 不处理自己
                        continue
                    }
                    /*
                 |****| j
                    |********| i
                 需要更新头
               */
                    if (jv[0] <= iv[0] && iv[1] >= jv[1] && jv[1] >=iv[0]) {
                        iv[0] = jv[0]
                        patches.removeAt(ji)
                        break@loop
                    }
                    /*
                               |****| j
                       |********| i
                     需要更新尾
                   */
                    if (jv[0] >= iv[0] && iv[1] <= jv[1] && iv[1] >= jv[0]) {
                        iv[1] = jv[1]
                        patches.removeAt(ji)
                        break@loop
                    }
                }

            }
            shouldloop = false
        }

    }

    private fun updatePatches(patch: IntArray) {
        patches.add(patch)
        mergePatches()
        // 找出 start<=avilableIndex<end 的end更新为available，这里的end就是可读到的位置了
        var minEnd = Int.MAX_VALUE
        for ((ji, jv) in patches.withIndex()) {
            if (jv[0] <= availableIndex && availableIndex < jv[1]) {
                minEnd = jv[1]
            }
        }
        if (minEnd != Int.MAX_VALUE) {
            availableIndex = minEnd
        }
    }

    @Synchronized
    override fun available(): Int {
        return availableIndex - cursor
    }
}