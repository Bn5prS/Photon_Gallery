package com.inferno.gallery.data.ai

/**
 * Single source of truth for the MobileFaceNet ONNX face recognition model.
 *
 * Architecture: MobileFaceNet (InsightFace w600k_mbf)
 * Input: 1 × 3 × 112 × 112 NCHW float (preprocessing: (pixel - 127.5) / 128.0)
 * Output: 1 × 512 float (L2-normalized embedding)
 */
object FaceModelRegistry {

    const val MODEL_DIR = "face_models_v1"
    const val MODEL_NAME = "w600k_mbf.onnx"
    const val MODEL_URL = "https://github.com/Bn5prS/Photon_Gallery/releases/download/models-v1/w600k_mbf.onnx"
    const val MODEL_SHA256 = "9cc6e4a75f0e2bf0b1aed94578f144d15175f357bdc05e815e5c4a02b319eb4f"
    const val MODEL_SIZE = 13_616_099L

    const val INPUT_NAME = "input.1"
    const val OUTPUT_NAME = "516"
    const val EMBEDDING_DIM = 512
}
