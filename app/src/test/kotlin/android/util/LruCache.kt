package android.util

/**
 * JVM-заглушка android.util.LruCache: в mockable android.jar все методы
 * бросают «not mocked». LinkedHashMap в режиме access-order с вытеснением
 * старейшего при превышении [maxSize] — семантика как в Android.
 */
open class LruCache<K, V>(private val maxSize: Int) {

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
            size > maxSize
    }

    fun get(key: K): V? = map[key]

    fun put(key: K, value: V): V? = map.put(key, value)

    fun size(): Int = map.size

    fun evictAll() {
        map.clear()
    }
}
