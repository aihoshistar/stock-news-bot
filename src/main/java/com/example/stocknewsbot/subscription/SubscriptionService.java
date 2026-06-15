package com.example.stocknewsbot.subscription;

import com.example.stocknewsbot.domain.Subscription;
import com.example.stocknewsbot.domain.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public boolean add(Long chatId, String stockCode, String stockName) {
        try {
            subscriptionRepository.save(new Subscription(chatId, stockCode, stockName));
            log.debug("구독 추가 chatId={} stockCode={}", chatId, stockCode);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("이미 구독 중 chatId={} stockCode={}", chatId, stockCode);
            return false;
        }
    }

    @Transactional
    public boolean remove(Long chatId, String stockCode) {
        if (subscriptionRepository.findByChatIdAndStockCode(chatId, stockCode).isEmpty()) {
            return false;
        }
        subscriptionRepository.deleteByChatIdAndStockCode(chatId, stockCode);
        log.debug("구독 취소 chatId={} stockCode={}", chatId, stockCode);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Subscription> list(Long chatId) {
        return subscriptionRepository.findByChatId(chatId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> findAll() {
        return subscriptionRepository.findAll();
    }
}