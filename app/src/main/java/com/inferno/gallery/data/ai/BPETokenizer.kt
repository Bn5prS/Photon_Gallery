package com.inferno.gallery.data.ai

import android.content.Context
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class BPETokenizer(jsonString: String) {
    val vocab: Map<String, Int>
    private val bpeRanks: Map<Pair<String, String>, Int>
    private val byteEncoder: Map<Int, Char>
    private val cache = mutableMapOf<String, String>()

    constructor(context: Context) : this(
        context.assets.open("tokenizer.json").bufferedReader().use { it.readText() }
    )

    init {
        val root = JSONObject(jsonString)
        val model = root.getJSONObject("model")
        
        val vocabObj = model.getJSONObject("vocab")
        val tempVocab = mutableMapOf<String, Int>()
        for (key in vocabObj.keys()) {
            tempVocab[key] = vocabObj.getInt(key)
        }
        vocab = tempVocab
        
        val mergesArr = model.getJSONArray("merges")
        val tempMerges = mutableMapOf<Pair<String, String>, Int>()
        for (i in 0 until mergesArr.length()) {
            val mergeStr = mergesArr.getString(i)
            val parts = mergeStr.split(" ")
            if (parts.size == 2) {
                tempMerges[Pair(parts[0], parts[1])] = i
            }
        }
        bpeRanks = tempMerges
        
        byteEncoder = buildByteEncoder()
    }

    private fun buildByteEncoder(): Map<Int, Char> {
        val bs = mutableListOf<Int>()
        bs.addAll('!'.code..'~'.code)
        bs.addAll('¡'.code..'¬'.code)
        bs.addAll('®'.code..'ÿ'.code)
        
        val cs = bs.toMutableList()
        var n = 0
        for (b in 0..255) {
            if (!bs.contains(b)) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }
        return bs.zip(cs).associate { it.first to it.second.toChar() }
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = mutableSetOf<Pair<String, String>>()
        var prevChar = word[0]
        for (i in 1 until word.size) {
            val char = word[i]
            pairs.add(Pair(prevChar, char))
            prevChar = char
        }
        return pairs
    }

    private fun indexOf(list: List<String>, element: String, startIndex: Int): Int {
        for (i in startIndex until list.size) {
            if (list[i] == element) return i
        }
        return -1
    }

    private fun bpe(token: String): String {
        if (cache.containsKey(token)) return cache[token]!!
        
        var word = if (token.endsWith("</w>")) {
            val chars = token.substring(0, token.length - 4).map { it.toString() }.toMutableList()
            if (chars.isNotEmpty()) {
                chars[chars.size - 1] = chars[chars.size - 1] + "</w>"
                chars
            } else {
                mutableListOf(token)
            }
        } else {
            token.map { it.toString() }.toList()
        }
        var pairs = getPairs(word)
        
        if (pairs.isEmpty()) return token
        
        while (true) {
            val bigram = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (!bpeRanks.containsKey(bigram)) break
            
            val first = bigram.first
            val second = bigram.second
            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                val j = indexOf(word, first, i)
                if (j == -1) {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }
                newWord.addAll(word.subList(i, j))
                i = j
                if (word[i] == first && i < word.size - 1 && word[i + 1] == second) {
                    newWord.add(first + second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i += 1
                }
            }
            word = newWord
            if (word.size == 1) break
            pairs = getPairs(word)
        }
        val result = word.joinToString(" ")
        cache[token] = result
        return result
    }

    fun encode(text: String): LongArray {
        val bpeTokens = mutableListOf<Int>()
        bpeTokens.add(49406) // <|startoftext|>
        
        val cleanText = text.lowercase()
        val pat = Pattern.compile("'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}]+")
        val matcher = pat.matcher(cleanText)
        
        while (matcher.find()) {
            val token = matcher.group()
            val utf8Bytes = token.toByteArray(StandardCharsets.UTF_8)
            val sb = StringBuilder()
            for (b in utf8Bytes) {
                val unsignedByte = b.toInt() and 0xFF
                sb.append(byteEncoder[unsignedByte])
            }
            // CLIP BPE requires </w> at the end of every token word
            sb.append("</w>")
            
            val bpeToken = bpe(sb.toString())
            for (bpeWord in bpeToken.split(" ")) {
                vocab[bpeWord]?.let { bpeTokens.add(it) }
            }
        }
        bpeTokens.add(49407) // <|endoftext|>
        return bpeTokens.map { it.toLong() }.toLongArray()
    }
}
