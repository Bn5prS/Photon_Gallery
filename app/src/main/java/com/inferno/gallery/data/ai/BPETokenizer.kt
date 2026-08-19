package com.inferno.gallery.data.ai

import org.json.JSONObject

class BPETokenizer(jsonContent: String) {

    val vocab: Map<String, Int>
    private val bpeRanks: Map<Pair<String, String>, Int>
    private val byteEncoder: Map<Byte, Char>
    private val startToken: Int
    private val endToken: Int

    init {
        val root = JSONObject(jsonContent)
        val model = root.getJSONObject("model")

        // Load Vocab
        val vocabJson = model.getJSONObject("vocab")
        val vocabMap = mutableMapOf<String, Int>()
        vocabJson.keys().forEach { key ->
            vocabMap[key] = vocabJson.getInt(key)
        }
        this.vocab = vocabMap

        // Load Merges
        val mergesJson = model.getJSONArray("merges")
        val ranksMap = mutableMapOf<Pair<String, String>, Int>()
        for (i in 0 until mergesJson.length()) {
            val mergeStr = mergesJson.getString(i)
            val parts = mergeStr.split(" ")
            if (parts.size == 2) {
                ranksMap[Pair(parts[0], parts[1])] = i
            }
        }
        this.bpeRanks = ranksMap

        // Create byte encoder
        this.byteEncoder = bytesToUnicode()

        // Get special tokens
        this.startToken = vocab["<|startoftext|>"] ?: 49406
        this.endToken = vocab["<|endoftext|>"] ?: 49407
    }

    private fun bytesToUnicode(): Map<Byte, Char> {
        val bs = mutableListOf<Int>()
        for (i in '!'.code..'~'.code) bs.add(i)
        for (i in '¡'.code..'¬'.code) bs.add(i)
        for (i in '®'.code..'ÿ'.code) bs.add(i)
        val cs = bs.toMutableList()
        var n = 0
        for (b in 0..255) {
            if (b !in bs) {
                bs.add(b)
                cs.add(256 + n)
                n++
            }
        }
        return bs.zip(cs).associate { (b, c) -> b.toByte() to c.toChar() }
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = mutableSetOf<Pair<String, String>>()
        if (word.size < 2) return pairs
        var prev = word[0]
        for (i in 1 until word.size) {
            val curr = word[i]
            pairs.add(Pair(prev, curr))
            prev = curr
        }
        return pairs
    }

    private fun bpe(token: String): String {
        if (token.isEmpty()) return ""

        // CLIP BPE attaches the end-of-word marker before merging. Adding it
        // after merging changes token IDs for common multi-subword terms.
        val bytes = token.toByteArray(Charsets.UTF_8)
        var word = bytes.map { b -> byteEncoder[b]?.toString() ?: "" }.toMutableList()
        if (word.isEmpty()) return ""
        word[word.lastIndex] = word.last() + "</w>"

        var pairs = getPairs(word)
        if (pairs.isEmpty()) return word.first()

        while (true) {
            val bigram = pairs.minByOrNull { bpeRanks[it] ?: Int.MAX_VALUE } ?: break
            if (bigram !in bpeRanks) break

            val first = bigram.first
            val second = bigram.second
            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                val indexInSubList = word.subList(i, word.size).indexOf(first)
                val j = if (indexInSubList == -1) -1 else i + indexInSubList
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
        return word.joinToString(" ")
    }

    fun encode(text: String): LongArray {
        val cleanedText = text.trim().lowercase()
            .replace("\\s+".toRegex(), " ")

        if (cleanedText.isEmpty()) {
            val emptyResult = LongArray(77)
            emptyResult[0] = startToken.toLong()
            emptyResult[1] = endToken.toLong()
            return emptyResult
        }

        // Standard CLIP regex split pattern
        val regex = "'s|'t|'re|'ve|'m|'ll|'d|\\p{L}+|\\p{N}|[^\\s\\p{L}\\p{N}]+".toRegex()
        val matches = regex.findAll(cleanedText).map { it.value }.toList()

        val tokenIds = mutableListOf<Int>()
        tokenIds.add(startToken)

        for (token in matches) {
            val bpeResult = bpe(token)
            val bpeTokens = bpeResult.split(" ")
            for (bpeToken in bpeTokens) {
                val id = vocab[bpeToken]
                if (id != null) {
                    tokenIds.add(id)
                }
            }
            if (tokenIds.size >= 76) break // Stop early to save space for endToken
        }

        // Ensure we don't exceed 77 tokens, leaving room for endToken
        if (tokenIds.size > 76) {
            tokenIds.subList(76, tokenIds.size).clear()
        }
        tokenIds.add(endToken)

        // Pad or truncate to sequence length 77
        val result = LongArray(77)
        for (i in 0 until 77) {
            result[i] = if (i < tokenIds.size) {
                tokenIds[i].toLong()
            } else {
                endToken.toLong() // Pad with <|endoftext|> (49407) in CLIP
            }
        }
        return result
    }
}
