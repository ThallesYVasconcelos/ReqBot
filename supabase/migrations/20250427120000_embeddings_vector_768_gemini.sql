-- Embeddings passam a usar Gemini API (768 dimensoes, Matryoshka).
-- Vetores antigos (384, ONNX AllMiniLM) sao incompativeis: limpar antes de alterar o tipo.

TRUNCATE TABLE embeddings;

ALTER TABLE embeddings
    ALTER COLUMN embedding TYPE vector(768);
