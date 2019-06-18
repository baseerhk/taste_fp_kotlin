package fkb.taste

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

fun filterOddsImperative(ns: Collection<Int>): Collection<Int> {
    val output = mutableListOf<Int>()
    for(n in ns) {
        if (n % 2 == 0) output.add(n)
    }
    return output
}

fun filterOdds(ns: Collection<Int>): Collection<Int> = if (ns.isEmpty()) listOf() else {
    val head = ns.take(1)[0]
    if (head % 2 == 0)
        listOf(head) + filterOdds(ns.drop(1))
    else
        filterOdds(ns.drop(1))
}

fun <T> filterRecursive(ts: Collection<T>, p: (t: T) -> Boolean): Collection<T> = if (ts.isEmpty()) listOf() else {
    val head = ts.take(1)[0]
    if (p(head))
        listOf(head) + filterRecursive(ts.drop(1), p)
    else
        filterRecursive(ts.drop(1), p)
}

fun <T> filterTCO(ts: Collection<T>, p: (t: T) -> Boolean): Collection<T> {
    fun inner(ts: Collection<T>, p: (t: T) -> Boolean, acc: List<T>): Collection<T> = if (ts.isEmpty()) acc else {
        val head = ts.take(1)[0]
        if (p(head))
            inner(ts.drop(1), p, acc + head)
        else
            inner(ts.drop(1), p, acc)
    }
    return inner(ts, p, listOf())
}

fun <A, B> fold(init: B, col: Collection<A>, f: (A, B) -> B): B = if (col.isEmpty()) init else {
    val head = col.take(1)[0]
    fold(f(head, init), col.drop(1), f)
}

fun <T> filter(ts: Collection<T>, p: (T) -> Boolean): Collection<T> =
        fold(listOf(), ts, {t, lst -> if(p(t)) lst + t else lst})

fun <T> length(ts: Collection<T>): Int = fold(0, ts, {_, acc -> acc + 1})

fun <T> reverse(ts: Collection<T>): Collection<T> = fold(listOf(), ts, {t, acc -> listOf(t) + acc})

fun <X, Y> map(xs: Collection<X>, f: (X) -> Y): Collection<Y> = fold(listOf(), xs, {x, acc -> acc + f(x)})

fun <T> traverse(root: Path, init: T, f: (Path, T) -> T): T =
    fold(init, Files.list(root).toList(),
        {path, acc -> if (! Files.isDirectory(path)) f(path, acc) else if (! Files.isSymbolicLink(path)) traverse(path, acc, f) else acc})

fun nGrams(n: Int, str: String): Collection<String> {
    fun inner(n: Int, str: String, acc: List<String>): Collection<String> =
        if (str.length < n) acc else
            inner(n, str.drop(1), acc + str.take(n))
    return inner(n, str, listOf())
}

fun allGrams(str: String): Collection<String> = fold(listOf(), (1..str.length).toList(), {n, acc -> acc + nGrams(n, str)})

fun normalize(s: String, stopList: Collection<Char>): String =
        fold("", s.toList(), {ch, acc -> if (! stopList.contains(ch)) acc + ch.toLowerCase() else acc})

fun <K, V> updateIndex(index: Map<K, Collection<V>>, pairs: Collection<Pair<K, V>>): Map<K, Collection<V>> =
        fold(index, pairs) { pair, acc ->
            val values = index[pair.first]
            when(values) {
                null -> acc + Pair(pair.first, listOf(pair.second))
                else -> acc + Pair(pair.first, values + pair.second)
            }
        }

fun <A, B> pairWithValue(col: Collection<A>, value: B): Collection<Pair<A, B>> =
        col.map {Pair(it,value)}

fun compIndex(path: Path): Map<String, Collection<Path>> =
        traverse(path, mapOf(), {path, acc ->
            updateIndex(acc, pairWithValue(allGrams(normalize(path.fileName.toString(), listOf('_', '-', '.', '$', ' '))), path.toAbsolutePath()))
        })

fun main(args: Array<String>) {
    assert(filterOddsImperative(listOf(1,2,3,4,5,6)) == listOf(2,4,6))
    assert(filterOddsImperative(listOf(2,6,8,0)) == listOf(2,6,8,0))
    assert(filterOdds(listOf(1,2,3,4,5,6)) == listOf(2,4,6))
    assert(filterRecursive(listOf(1,2,3,4,5,6)){it % 2 == 0} == listOf(2,4,6))
    assert(filterRecursive(listOf("this", "is", "a", "Good", "Idea")) {it.length > 1} == listOf("this", "is", "Good", "Idea"))
    assert(filterTCO(listOf(1,2,3,4,5,6)) {it % 2 == 0} == listOf(2,4,6))
    assert(filter(listOf(1,2,3,4,5,6)) {it % 2 == 0} == listOf(2,4,6))
    assert(length(listOf<Int>()) == 0)
    assert(length(listOf(1,2,3,4,5,6,7,8)) == 8)
    assert(reverse(listOf<Any>()) == listOf<Any>())
    assert(reverse(listOf(1,2,3)) == listOf(3,2,1))
    assert(map(listOf("abc", "d", "xy", "")) {it.length} == listOf(3, 1, 2, 0))

    val f = {path: Path, acc: String -> "$acc\n${path.toAbsolutePath()}"}
    val g = {path: Path, acc: Long -> acc + Files.size(path)}
    println(traverse(Paths.get(args[0]), "", f))
    println(traverse(Paths.get(args[0]), 0, g))

    assert(nGrams(1, "from") == listOf("f", "r", "o", "m"))
    assert(nGrams(3, "from") == listOf("fro", "rom"))
    assert(nGrams(4, "from") == listOf("from"))
    assert(nGrams(5, "from") == listOf<String>())
    assert(allGrams("from") == listOf("f", "r", "o", "m", "fr", "ro", "om", "fro", "rom", "from"))
    assert(normalize("mY_file.TXT", listOf('_', '-', '.', ' ')) == "myfiletxt")

    val index = compIndex(Paths.get(args[0]))
    println(index["ds"])
}