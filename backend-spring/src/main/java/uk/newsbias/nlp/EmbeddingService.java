package uk.newsbias.nlp;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embeds text using all-MiniLM-L6-v2 via ONNX Runtime + DJL tokenizers.
 *
 * One-time setup (run once before first launch):
 *   pip install optimum[onnxruntime]
 *   optimum-cli export onnx \
 *     --model sentence-transformers/all-MiniLM-L6-v2 \
 *     --task feature-extraction \
 *     src/main/resources/model/
 */
@Slf4j
@Service
public class EmbeddingService {

    private static final int DIM = 384;

    @Value("${app.model-path:src/main/resources/model}")
    private String modelPath;

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    @PostConstruct
    public void init() throws OrtException, IOException {
        Path base = Paths.get(modelPath);
        Path modelFile     = base.resolve("model.onnx");
        Path tokenizerFile = base.resolve("tokenizer.json");

        if (!modelFile.toFile().exists() || !tokenizerFile.toFile().exists()) {
            throw new IllegalStateException(
                "ONNX model not found at " + base.toAbsolutePath() + ". " +
                "Run the one-time export:\n" +
                "  pip install optimum[onnxruntime]\n" +
                "  optimum-cli export onnx --model sentence-transformers/all-MiniLM-L6-v2 " +
                "--task feature-extraction " + base.toAbsolutePath()
            );
        }

        env      = OrtEnvironment.getEnvironment();
        session  = env.createSession(modelFile.toString(), new OrtSession.SessionOptions());
        tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
        log.info("EmbeddingService ready — model: {}", modelFile);
    }

    @PreDestroy
    public void close() throws OrtException {
        if (session  != null) session.close();
        if (env      != null) env.close();
        if (tokenizer != null) tokenizer.close();
    }

    /**
     * Embed a list of texts. Returns a list of normalised float[] embeddings (dim=384).
     */
    public List<float[]> embed(List<String> texts) throws OrtException {
        return texts.stream().map(text -> {
            try {
                return embedOne(text);
            } catch (OrtException e) {
                throw new RuntimeException("Embedding failed for text: " + text, e);
            }
        }).toList();
    }

    /**
     * Convenience: embed a single text.
     */
    public float[] embedOne(String text) throws OrtException {
        Encoding encoding = tokenizer.encode(text);  // special tokens added by default

        long[] inputIds      = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds  = encoding.getTypeIds();
        long seqLen = inputIds.length;
        long[] shape = {1, seqLen};

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids",      OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds),      shape));
        inputs.put("attention_mask", OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape));
        inputs.put("token_type_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds),  shape));

        try (OrtSession.Result result = session.run(inputs)) {
            // last_hidden_state: shape [1, seqLen, 384]
            float[][][] hidden = (float[][][]) result.get("last_hidden_state")
                    .orElseThrow()
                    .getValue();

            float[] embedding = meanPool(hidden[0], attentionMask);
            l2Normalize(embedding);
            return embedding;
        } finally {
            inputs.values().forEach(OnnxTensor::close);
        }
    }

    // ------------------------------------------------------------------ utils

    /** Attention-mask-weighted mean pooling over sequence dimension. */
    private float[] meanPool(float[][] hidden, long[] mask) {
        float[] result = new float[DIM];
        long sum = 0;
        for (int t = 0; t < hidden.length; t++) {
            if (mask[t] == 1) {
                for (int d = 0; d < DIM; d++) result[d] += hidden[t][d];
                sum++;
            }
        }
        if (sum > 0) {
            for (int d = 0; d < DIM; d++) result[d] /= sum;
        }
        return result;
    }

    private void l2Normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < v.length; i++) v[i] /= norm;
    }

    // ------------------------------------------------------------------ serialisation

    public static byte[] toBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }

    public static float[] fromBytes(byte[] b) {
        ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        float[] v = new float[b.length / 4];
        for (int i = 0; i < v.length; i++) v[i] = buf.getFloat();
        return v;
    }

    public static float cosineSimilarity(float[] a, float[] b) {
        // Both are already L2-normalised, so dot product == cosine sim
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
