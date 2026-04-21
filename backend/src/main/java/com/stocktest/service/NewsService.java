package com.stocktest.service;

import com.stocktest.model.NewsItem;
import com.stocktest.model.StockMentionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.StringReader;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final RestTemplate restTemplate;
    private final StockService stockService;
    private final YoutubeService youtubeService;

    // 30분 캐시
    private List<NewsItem> cachedNews = null;
    private long cacheTime = 0;
    private static final long CACHE_MILLIS = 30 * 60 * 1000L;

    // Google News RSS URL (한국 증시 관련 키워드)
    private static final List<String> RSS_QUERIES = List.of(
            "삼성전자 주가",
            "SK하이닉스 주가",
            "코스피 급등 종목 주식",
            "한국 증시 추천 종목"
    );

    public List<NewsItem> getNewsAnalysis(boolean forceRefresh) {
        if (!forceRefresh && cachedNews != null &&
                System.currentTimeMillis() - cacheTime < CACHE_MILLIS) {
            return cachedNews;
        }

        Map<String, NewsItem> dedup = new LinkedHashMap<>();

        for (String query : RSS_QUERIES) {
            try {
                List<NewsItem> items = fetchRss(query);
                for (NewsItem item : items) {
                    dedup.putIfAbsent(item.getLink(), item);
                }
            } catch (Exception e) {
                log.warn("RSS 조회 실패: {}", query, e);
            }
        }

        // 종목별로 가장 최근 기사 1건만 유지 (중복 종목 기사 제거)
        Set<String> seenStockSets = new LinkedHashSet<>();
        List<NewsItem> deduped = new ArrayList<>();
        for (NewsItem item : dedup.values()) {
            if (item.getRelatedStocks().isEmpty()) {
                deduped.add(item);
            } else {
                String stockKey = item.getRelatedStocks().stream()
                        .map(StockMentionResult::getCode)
                        .sorted()
                        .collect(Collectors.joining(","));
                if (seenStockSets.add(stockKey)) {
                    deduped.add(item);
                }
            }
        }

        // 종목 언급 있는 기사만 반환
        List<NewsItem> result = deduped.stream()
                .filter(n -> !n.getRelatedStocks().isEmpty())
                .limit(30)
                .collect(Collectors.toList());

        cachedNews = result;
        cacheTime = System.currentTimeMillis();
        return result;
    }

    private List<NewsItem> fetchRss(String query) throws Exception {
        String encoded = java.net.URLEncoder.encode(query, "UTF-8");
        String url = "https://news.google.com/rss/search?q=" + encoded + "&hl=ko&gl=KR&ceid=KR:ko";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        String xml = resp.getBody();
        if (xml == null) return List.of();

        return parseRss(xml);
    }

    private List<NewsItem> parseRss(String xml) {
        List<NewsItem> items = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList nodeList = doc.getElementsByTagName("item");
            for (int i = 0; i < Math.min(nodeList.getLength(), 20); i++) {
                Element el = (Element) nodeList.item(i);
                String title = textOf(el, "title");
                String link  = textOf(el, "link");
                String pubDate = textOf(el, "pubDate");
                String source = textOf(el, "source");
                if (source == null || source.isBlank()) {
                    source = extractSource(title);
                    title  = cleanTitle(title);
                }

                List<StockMentionResult> stocks = extractStocks(title);

                items.add(NewsItem.builder()
                        .title(title)
                        .link(link)
                        .source(source)
                        .publishedAt(formatPubDate(pubDate))
                        .relatedStocks(stocks)
                        .build());
            }
        } catch (Exception e) {
            log.warn("RSS 파싱 실패", e);
        }
        return items;
    }

    private List<StockMentionResult> extractStocks(String text) {
        if (text == null) return List.of();
        Set<String> foundCodes = new LinkedHashSet<>();

        // 1. 종목명 직접 매칭
        for (Map.Entry<String, String> entry : YoutubeService.STOCK_ALIAS.entrySet()) {
            if (text.contains(entry.getKey())) {
                foundCodes.add(entry.getValue());
            }
        }

        // 2. 테마 키워드 매칭 (AI, 반도체 등)
        for (Map.Entry<String, List<String>> entry : YoutubeService.THEME_MAP.entrySet()) {
            if (text.contains(entry.getKey())) {
                foundCodes.addAll(entry.getValue());
            }
        }

        List<StockMentionResult> result = new ArrayList<>();
        for (String code : foundCodes) {
            try {
                var price = stockService.getPrice(code);
                String name = (price.getName() != null && !price.getName().isBlank())
                        ? price.getName()
                        : YoutubeService.CODE_TO_NAME.getOrDefault(code, code);
                result.add(StockMentionResult.builder()
                        .code(code).name(name)
                        .currentPrice(price.getCurrentPrice())
                        .change(price.getChange())
                        .changeRate(price.getChangeRate())
                        .up(price.isUp())
                        .build());
            } catch (Exception e) {
                log.warn("가격 조회 실패: {}", code);
            }
        }
        return result;
    }

    private String textOf(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent().trim();
    }

    // Google News title format: "기사제목 - 언론사명"
    private String extractSource(String title) {
        if (title == null) return "";
        int idx = title.lastIndexOf(" - ");
        return idx >= 0 ? title.substring(idx + 3).trim() : "";
    }

    private String cleanTitle(String title) {
        if (title == null) return "";
        int idx = title.lastIndexOf(" - ");
        return idx >= 0 ? title.substring(0, idx).trim() : title;
    }

    private String formatPubDate(String pubDate) {
        if (pubDate == null) return "";
        try {
            // RFC_1123 format: "Mon, 21 Apr 2026 08:30:00 GMT"
            ZonedDateTime zdt = ZonedDateTime.parse(pubDate,
                    DateTimeFormatter.RFC_1123_DATE_TIME);
            ZonedDateTime kst = zdt.withZoneSameInstant(ZoneId.of("Asia/Seoul"));
            return kst.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
        } catch (Exception e) {
            return pubDate.length() > 16 ? pubDate.substring(0, 16) : pubDate;
        }
    }

    public void clearCache() {
        cachedNews = null;
        cacheTime = 0;
    }
}
