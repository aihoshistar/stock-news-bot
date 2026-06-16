package com.example.stocknewsbot.news;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TfIdfSimilarityFilter {
    private static final double SIMILARITY_THRESHOLD = 0.7;

    public List<Integer> filterSimilar(List<String> titles) {
        if (titles.size() <= 1) {
            return titles.isEmpty() ? List.of()  : List.of(0);
        }

        // 각 제목을 단어 집합으로 토큰화
        List<List<String>> tokenized = titles.stream().map(this::tokenize).toList();

        // 전체 단어 사전 구성
        Set<String> vocabulary = new HashSet<>();
        tokenized.forEach(vocabulary::addAll);
        List<String> vocabList = new ArrayList<>(vocabulary);

        // IDF 계산
        Map<String, Double> idf = computeIdf(tokenized, vocabList);

        // 각 제목의 TF-IDF 벡터 계산
        List<Map<String, Double>> vectors = tokenized.stream()
                .map(tokens -> computeTfIdf(tokens, vocabList, idf)).toList();

        // 유사도 비교 후 대표 인덱스 선택
        List<Integer> selected = new ArrayList<>();
        boolean[] excluded = new boolean[titles.size()];

        for (int i = 0; i < titles.size(); i++) {
            if (excluded[i]) continue;
            selected.add(i);

            for (int j = 0; j < titles.size(); j++) {
                if (excluded[j]) continue;
                double similarity = cosineSimilarity(vectors.get(i), vectors.get(j));
                if (similarity >= SIMILARITY_THRESHOLD) {
                    excluded[j] = true;
                }
            }
        }

        return selected;
    }

    private List<String> tokenize(String title) {
        return Arrays.stream(title
                .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ")
                .toLowerCase()
                .split("\\s+"))
                .filter(token -> token.length() >=2)
                .toList();
    }

    private Map<String, Double> computeIdf(List<List<String>> tokenized, List<String> vocabList) {
        int docCount = tokenized.size();
        Map<String, Double> idf = new HashMap<>();

        for (String term : vocabList) {
            long docFreq = tokenized.stream().filter(tokens -> tokens.contains(term)).count();
            idf.put(term, Math.log((double) docCount / (docFreq + 1)) + 1);
        }
        return idf;
    }

    private Map<String, Double> computeTfIdf(List<String> tokens, List<String> vocabList, Map<String, Double> idf) {
        Map<String, Double> tfIdf = new HashMap<>();
        int totalTerms = tokens.size();

        for (String term : vocabList) {
            long termCount = tokens.stream().filter(t -> t.equals(term)).count();
            double tf = totalTerms == 0 ? 0 : (double) termCount / totalTerms;

            // IDF = log(전체 문서 수 / 해당 단어 포함 문서 수 + 1)
            tfIdf.put(term, tf * idf.getOrDefault(term, 0.0));
        }

        return tfIdf;
    }

    private double cosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : vectorA.keySet()) {
            double a = vectorA.getOrDefault(key, 0.0);
            double b = vectorB.getOrDefault(key, 0.0);
            dotProduct += a * b;
            normA += a * a;
        }

        for (double b : vectorB.values()) {
            normB += b * b;
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
