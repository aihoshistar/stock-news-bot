package com.example.stocknewsbot.news;

import com.example.stocknewsbot.ai.AiClient;
import com.example.stocknewsbot.ai.AiClient.NewsAnalysis;
import com.example.stocknewsbot.ai.AiClient.Sentiment;
import com.example.stocknewsbot.domain.SentNews;
import com.example.stocknewsbot.domain.SentNewsRepository;
import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.subscription.SubscriptionService;
import com.example.stocknewsbot.telegram.TelegramClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock NaverNewsClient naverNewsClient;
    @Mock AiClient aiClient;
    @Mock TfIdfSimilarityFilter similarityFilter;
    @Mock SentNewsRepository sentNewsRepository;
    @Mock SubscriptionService subscriptionService;
    @Mock TelegramClient telegramClient;

    @InjectMocks
    NewsService newsService;

    private Subscription subscription;
    private NewsAnalysis mockAnalysis;

    @BeforeEach
    void setUp() {
        subscription = new Subscription(123L, "005930", "삼성전자");
        mockAnalysis = new NewsAnalysis("삼성전자 실적 호조", Sentiment.POSITIVE);
    }

    @Test
    @DisplayName("구독이 없으면 뉴스를 조회하지 않는다")
    void pollAndSend_noSubscriptions_doesNothing() {
        given(subscriptionService.findAll()).willReturn(List.of());

        newsService.pollAndSend();

        verify(naverNewsClient, never()).searchNews(any());
    }

    @Test
    @DisplayName("새 뉴스는 AI 분석 후 텔레그램으로 발송하고 이력을 저장한다")
    void pollAndSend_newNews_sendsAndSaves() {
        // given
        Map<String, Object> news = Map.of(
                "title", "삼성전자 2분기 영업이익 급증",
                "link", "https://news.example.com/1"
        );
        given(subscriptionService.findAll()).willReturn(List.of(subscription));
        given(naverNewsClient.searchNews("삼성전자")).willReturn(List.of(news));
        given(sentNewsRepository.existsByLinkHash(any())).willReturn(false);
        given(similarityFilter.filterSimilar(any())).willReturn(List.of(0));
        given(aiClient.analyze(eq("삼성전자"), any())).willReturn(mockAnalysis);

        // when
        newsService.pollAndSend();

        // then
        verify(telegramClient, times(1)).sendMessage(eq(123L), anyString());
        verify(sentNewsRepository, times(1)).save(any(SentNews.class));
    }

    @Test
    @DisplayName("이미 발송한 뉴스는 다시 발송하지 않는다")
    void pollAndSend_alreadySentNews_skips() {
        // given
        Map<String, Object> news = Map.of(
                "title", "삼성전자 2분기 영업이익 급증",
                "link", "https://news.example.com/1"
        );
        given(subscriptionService.findAll()).willReturn(List.of(subscription));
        given(naverNewsClient.searchNews("삼성전자")).willReturn(List.of(news));
        given(sentNewsRepository.existsByLinkHash(any())).willReturn(true);

        // when
        newsService.pollAndSend();

        // then
        verify(telegramClient, never()).sendMessage(anyLong(), anyString());
        verify(sentNewsRepository, never()).save(any());
    }

    @Test
    @DisplayName("유사 뉴스는 대표 1건만 발송한다")
    void pollAndSend_similarNews_sendsOnlyRepresentative() {
        // given
        Map<String, Object> news1 = Map.of(
                "title", "삼성전자 영업이익 급증 어닝서프라이즈",
                "link", "https://news.example.com/1"
        );
        Map<String, Object> news2 = Map.of(
                "title", "삼성전자 영업이익 깜짝 상승 서프라이즈",
                "link", "https://news.example.com/2"
        );
        given(subscriptionService.findAll()).willReturn(List.of(subscription));
        given(naverNewsClient.searchNews("삼성전자")).willReturn(List.of(news1, news2));
        given(sentNewsRepository.existsByLinkHash(any())).willReturn(false);
        // 유사도 필터가 첫 번째 뉴스만 선택
        given(similarityFilter.filterSimilar(any())).willReturn(List.of(0));
        given(aiClient.analyze(eq("삼성전자"), any())).willReturn(mockAnalysis);

        // when
        newsService.pollAndSend();

        // then: 2건 중 1건만 발송
        verify(telegramClient, times(1)).sendMessage(eq(123L), anyString());
        verify(sentNewsRepository, times(1)).save(any(SentNews.class));
    }

    @Test
    @DisplayName("발송 메시지에 종목명, 요약, 링크가 포함된다")
    void pollAndSend_messageContainsExpectedContent() {
        // given
        Map<String, Object> news = Map.of(
                "title", "삼성전자 2분기 영업이익 급증",
                "link", "https://news.example.com/1"
        );
        given(subscriptionService.findAll()).willReturn(List.of(subscription));
        given(naverNewsClient.searchNews("삼성전자")).willReturn(List.of(news));
        given(sentNewsRepository.existsByLinkHash(any())).willReturn(false);
        given(similarityFilter.filterSimilar(any())).willReturn(List.of(0));
        given(aiClient.analyze(eq("삼성전자"), any())).willReturn(mockAnalysis);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // when
        newsService.pollAndSend();

        // then
        verify(telegramClient).sendMessage(eq(123L), messageCaptor.capture());
        String message = messageCaptor.getValue();
        assertThat(message).contains("삼성전자");
        assertThat(message).contains("005930");
        assertThat(message).contains("삼성전자 실적 호조");
        assertThat(message).contains("https://news.example.com/1");
    }
}