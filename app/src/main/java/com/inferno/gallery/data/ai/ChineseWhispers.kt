package com.inferno.gallery.data.ai

import java.util.Random

/**
 * Pure Kotlin implementation of the Chinese Whispers graph community detection algorithm.
 *
 * Runs in O(V + E) linear time. Discovers natural community partitions without requiring
 * a pre-specified cluster count (K).
 */
object ChineseWhispers {

    data class Edge(val target: Long, val weight: Float)

    /**
     * Partitions nodes into clusters based on weighted edge connectivity.
     *
     * @param nodes List of node identifiers (e.g. face IDs)
     * @param edges Adjacency map of node -> list of outgoing edges (undirected graph)
     * @param maxIterations Number of label propagation passes (default: 15)
     * @param seed Optional random seed for reproducible node shuffling during iteration
     * @return Map of NodeId -> ClusterId
     */
    fun cluster(
        nodes: List<Long>,
        edges: Map<Long, List<Edge>>,
        maxIterations: Int = 15,
        seed: Long = 42L
    ): Map<Long, Long> {
        if (nodes.isEmpty()) return emptyMap()

        // 1. Initial condition: each node starts with its own label
        val labels = HashMap<Long, Long>(nodes.size)
        for (node in nodes) {
            labels[node] = node
        }

        val rng = Random(seed)
        val shuffledNodes = nodes.toMutableList()

        // 2. Iterative label propagation
        for (iter in 0 until maxIterations) {
            var changed = false
            // Shuffle node evaluation order to prevent directional bias
            shuffledNodes.shuffle(rng)

            for (node in shuffledNodes) {
                val neighbors = edges[node] ?: continue
                if (neighbors.isEmpty()) continue

                // Accumulate edge weights grouped by neighbor label
                val labelWeights = HashMap<Long, Float>()
                for (edge in neighbors) {
                    val neighborLabel = labels[edge.target] ?: edge.target
                    val currentWeight = labelWeights[neighborLabel] ?: 0f
                    labelWeights[neighborLabel] = currentWeight + edge.weight
                }

                // Choose the label with highest accumulated weight
                // Deterministic tie-breaking by smallest label ID
                var bestLabel = labels[node] ?: node
                var maxWeight = -1f

                for ((label, weight) in labelWeights) {
                    if (weight > maxWeight || (weight == maxWeight && label < bestLabel)) {
                        maxWeight = weight
                        bestLabel = label
                    }
                }

                if (labels[node] != bestLabel) {
                    labels[node] = bestLabel
                    changed = true
                }
            }

            if (!changed) break
        }

        return labels
    }

    /**
     * Helper to group the result into ClusterId -> List of NodeIds.
     */
    fun groupClusters(nodeToCluster: Map<Long, Long>): Map<Long, List<Long>> {
        val clusters = HashMap<Long, MutableList<Long>>()
        for ((node, cluster) in nodeToCluster) {
            clusters.getOrPut(cluster) { mutableListOf() }.add(node)
        }
        return clusters
    }
}
