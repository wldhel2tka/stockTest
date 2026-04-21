package com.stocktest.service;

import com.stocktest.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeService {

    private final RestTemplate restTemplate;
    private final StockService stockService;

    @Value("${youtube.api-key}")
    private String apiKey;

    private static final String YT_BASE = "https://www.googleapis.com/youtube/v3";

    // 분석 캐시 (30분)
    private final Map<String, YoutubeChannelAnalysis> cache = new LinkedHashMap<>();
    private final Map<String, Long> cacheTime = new LinkedHashMap<>();
    private static final long CACHE_MILLIS = 30 * 60 * 1000L;

    // 유튜버 목록 (channelId 직접 지정)
    private static final List<YoutubeChannel> CHANNELS = List.of(
        YoutubeChannel.builder()
            .id("1").name("소수몽키").handle("sosumonkey").channelId("UCC3yfxS5qC6PCwDzetUuEWg")
            .description("가치투자 기반 국내주식 분석").build(),
        YoutubeChannel.builder()
            .id("2").name("올랜도 킴").handle("orlandokim").channelId("UCwSSqi-s0wcH6pJbH3YPZqQ")
            .description("미국·한국 주식 실전 투자").build(),
        YoutubeChannel.builder()
            .id("3").name("주식의 코드").handle("stockcode").channelId("UCxKabFFGCD9kVyrEWgPPLNA")
            .description("차트·수급 기반 국내주식 분석").build(),
        YoutubeChannel.builder()
            .id("4").name("삼프로TV").handle("3protv").channelId("UChlv4GSd7OQl3js-jkLOnFA")
            .description("경제의신과함께 - 주식·경제 전문 채널").build(),
        YoutubeChannel.builder()
            .id("5").name("주식코치TV").handle("stockcoach").channelId("UC5vbtx5ZZf9fcNkC0TuLA6w")
            .description("김혜산 주식코치 - 실전 투자 전략").build(),
        YoutubeChannel.builder()
            .id("6").name("슈카월드").handle("ShukaWorld").channelId("UCsJ6RuBiTVWRX156FVbeaGg")
            .description("세상의 경제 이야기, 슈카월드").build()
    );

    // 종목명 → 종목코드 (StockService STOCK_LIST 기반 + 별칭)
    public static final Map<String, String> STOCK_ALIAS = new LinkedHashMap<>();
    static {
        // KOSPI 주요 종목 별칭
        STOCK_ALIAS.put("삼성전자", "005930");
        STOCK_ALIAS.put("삼전", "005930");
        STOCK_ALIAS.put("SK하이닉스", "000660");
        STOCK_ALIAS.put("하이닉스", "000660");
        STOCK_ALIAS.put("LG에너지솔루션", "373220");
        STOCK_ALIAS.put("LGES", "373220");
        STOCK_ALIAS.put("삼성바이오로직스", "207940");
        STOCK_ALIAS.put("삼바", "207940");
        STOCK_ALIAS.put("현대차", "005380");
        STOCK_ALIAS.put("현대자동차", "005380");
        STOCK_ALIAS.put("기아", "000270");
        STOCK_ALIAS.put("기아차", "000270");
        STOCK_ALIAS.put("POSCO홀딩스", "005490");
        STOCK_ALIAS.put("포스코홀딩스", "005490");
        STOCK_ALIAS.put("포스코", "005490");
        STOCK_ALIAS.put("삼성SDI", "006400");
        STOCK_ALIAS.put("LG화학", "051910");
        STOCK_ALIAS.put("셀트리온", "068270");
        STOCK_ALIAS.put("KB금융", "105560");
        STOCK_ALIAS.put("신한지주", "055550");
        STOCK_ALIAS.put("신한금융", "055550");
        STOCK_ALIAS.put("하나금융지주", "086790");
        STOCK_ALIAS.put("하나금융", "086790");
        STOCK_ALIAS.put("우리금융지주", "316140");
        STOCK_ALIAS.put("우리금융", "316140");
        STOCK_ALIAS.put("삼성물산", "028260");
        STOCK_ALIAS.put("LG전자", "066570");
        STOCK_ALIAS.put("카카오", "035720");
        STOCK_ALIAS.put("네이버", "035420");
        STOCK_ALIAS.put("NAVER", "035420");
        STOCK_ALIAS.put("크래프톤", "259960");
        STOCK_ALIAS.put("삼성생명", "032830");
        STOCK_ALIAS.put("HD현대중공업", "329180");
        STOCK_ALIAS.put("현대중공업", "329180");
        STOCK_ALIAS.put("HMM", "011200");
        STOCK_ALIAS.put("두산에너빌리티", "034020");
        STOCK_ALIAS.put("포스코퓨처엠", "003670");
        STOCK_ALIAS.put("삼성에스디에스", "018260");
        STOCK_ALIAS.put("삼성SDS", "018260");
        STOCK_ALIAS.put("삼성전기", "009150");
        STOCK_ALIAS.put("삼성중공업", "028050");
        STOCK_ALIAS.put("고려아연", "010130");
        STOCK_ALIAS.put("현대제철", "004020");
        STOCK_ALIAS.put("삼성화재", "000810");
        STOCK_ALIAS.put("아모레퍼시픽", "090430");
        STOCK_ALIAS.put("아모레", "090430");
        STOCK_ALIAS.put("KT&G", "033780");
        STOCK_ALIAS.put("LG이노텍", "011070");
        STOCK_ALIAS.put("기업은행", "024110");
        STOCK_ALIAS.put("KT", "030200");
        STOCK_ALIAS.put("SK텔레콤", "017670");
        STOCK_ALIAS.put("SKT", "017670");
        STOCK_ALIAS.put("LGU+", "032640");
        STOCK_ALIAS.put("LG유플러스", "032640");
        STOCK_ALIAS.put("현대건설", "000720");
        STOCK_ALIAS.put("SK이노베이션", "096770");
        STOCK_ALIAS.put("포스코인터내셔널", "047050");
        STOCK_ALIAS.put("한국전력", "015760");
        STOCK_ALIAS.put("한전", "015760");
        STOCK_ALIAS.put("유한양행", "000100");
        STOCK_ALIAS.put("HD한국조선해양", "009540");
        STOCK_ALIAS.put("한국조선해양", "009540");
        STOCK_ALIAS.put("한화에어로스페이스", "012450");
        STOCK_ALIAS.put("한화에어로", "012450");
        STOCK_ALIAS.put("HD현대일렉트릭", "267260");
        STOCK_ALIAS.put("현대일렉트릭", "267260");
        STOCK_ALIAS.put("LS일렉트릭", "010120");
        STOCK_ALIAS.put("효성중공업", "298040");
        STOCK_ALIAS.put("한화오션", "042660");
        STOCK_ALIAS.put("대한항공", "003490");
        STOCK_ALIAS.put("카카오뱅크", "323410");
        STOCK_ALIAS.put("메리츠금융지주", "138040");
        STOCK_ALIAS.put("메리츠금융", "138040");
        STOCK_ALIAS.put("현대모비스", "012330");
        STOCK_ALIAS.put("현대글로비스", "086280");
        STOCK_ALIAS.put("LG", "003550");
        STOCK_ALIAS.put("SK", "034730");
        // KOSDAQ 주요 종목
        STOCK_ALIAS.put("에코프로비엠", "247540");
        STOCK_ALIAS.put("에코프로", "086520");
        STOCK_ALIAS.put("셀트리온헬스케어", "091990");
        STOCK_ALIAS.put("알테오젠", "196170");
        STOCK_ALIAS.put("카카오게임즈", "293490");
        STOCK_ALIAS.put("클래시스", "214150");
        STOCK_ALIAS.put("하이브", "352820");
        STOCK_ALIAS.put("에스엠", "041510");
        STOCK_ALIAS.put("SM엔터", "041510");
        STOCK_ALIAS.put("JYP", "035900");
        STOCK_ALIAS.put("펄어비스", "263750");
        STOCK_ALIAS.put("휴젤", "145020");
        STOCK_ALIAS.put("솔브레인", "357780");
        STOCK_ALIAS.put("두산로보틱스", "454910");
        STOCK_ALIAS.put("레인보우로보틱스", "277810");
        STOCK_ALIAS.put("HLB", "028300");
        STOCK_ALIAS.put("한미약품", "128940");
        STOCK_ALIAS.put("리가켐바이오", "141080");
        STOCK_ALIAS.put("더존비즈온", "012510");
        STOCK_ALIAS.put("HPSP", "403870");
        STOCK_ALIAS.put("리노공업", "058470");
        STOCK_ALIAS.put("원익IPS", "240810");
        STOCK_ALIAS.put("이오테크닉스", "039030");
        STOCK_ALIAS.put("주성엔지니어링", "036930");
        STOCK_ALIAS.put("넷마블", "251270");
    }

    public List<YoutubeChannel> getChannels() {
        return CHANNELS;
    }

    public List<YoutubeChannelAnalysis> getFullAnalysis() {
        return CHANNELS.stream()
                .map(this::analyzeChannel)
                .collect(Collectors.toList());
    }

    private YoutubeChannelAnalysis analyzeChannel(YoutubeChannel channel) {
        String cid = channel.getId();

        // 캐시 확인
        if (cache.containsKey(cid)) {
            long elapsed = System.currentTimeMillis() - cacheTime.getOrDefault(cid, 0L);
            if (elapsed < CACHE_MILLIS) {
                return cache.get(cid);
            }
        }

        if ("YOUR_YOUTUBE_API_KEY_HERE".equals(apiKey)) {
            return YoutubeChannelAnalysis.builder()
                    .channel(channel)
                    .recentVideos(List.of())
                    .mentionedStocks(List.of())
                    .error("YouTube API 키를 application.yml에 설정해주세요.")
                    .build();
        }

        try {
            // 1. 채널 썸네일 조회 (channelId 이미 알고 있으면 바로 사용)
            YoutubeChannel enriched = channel.getChannelId() != null
                    ? fetchChannelThumbnail(channel)
                    : fetchChannelInfo(channel);

            // 2. 최근 영상 1개 조회
            List<YoutubeVideoInfo> videos = fetchRecentVideos(enriched.getChannelId(), 1);

            // 3. 영상 제목 + 설명에서 종목 추출
            Set<String> codes = extractStockCodes(videos);

            // 4. KIS 현재가 조회
            List<StockMentionResult> stocks = fetchPrices(codes);

            YoutubeChannelAnalysis result = YoutubeChannelAnalysis.builder()
                    .channel(enriched)
                    .recentVideos(videos)
                    .mentionedStocks(stocks)
                    .build();

            cache.put(cid, result);
            cacheTime.put(cid, System.currentTimeMillis());
            return result;

        } catch (Exception e) {
            log.error("채널 분석 실패: {}", channel.getName(), e);
            return YoutubeChannelAnalysis.builder()
                    .channel(channel)
                    .recentVideos(List.of())
                    .mentionedStocks(List.of())
                    .error("분석 중 오류: " + e.getMessage())
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private YoutubeChannel fetchChannelInfo(YoutubeChannel channel) {
        String url = YT_BASE + "/channels?part=snippet&forHandle=" + channel.getHandle() + "&key=" + apiKey;
        ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) return channel;

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return channel;

        Map<String, Object> item = items.get(0);
        String channelId = (String) item.get("id");
        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
        Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
        Map<String, Object> defaultThumb = (Map<String, Object>) thumbnails.get("default");
        String thumb = defaultThumb != null ? (String) defaultThumb.get("url") : null;

        return YoutubeChannel.builder()
                .id(channel.getId())
                .name(channel.getName())
                .handle(channel.getHandle())
                .description(channel.getDescription())
                .channelId(channelId)
                .thumbnailUrl(thumb)
                .build();
    }

    @SuppressWarnings("unchecked")
    private YoutubeChannel fetchChannelThumbnail(YoutubeChannel channel) {
        try {
            String url = YT_BASE + "/channels?part=snippet&id=" + channel.getChannelId() + "&key=" + apiKey;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null) return channel;
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) return channel;
            Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");
            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
            Map<String, Object> defaultThumb = (Map<String, Object>) thumbnails.get("default");
            String thumb = defaultThumb != null ? (String) defaultThumb.get("url") : null;
            return YoutubeChannel.builder()
                    .id(channel.getId()).name(channel.getName()).handle(channel.getHandle())
                    .description(channel.getDescription()).channelId(channel.getChannelId())
                    .thumbnailUrl(thumb).build();
        } catch (Exception e) {
            return channel;
        }
    }

    @SuppressWarnings("unchecked")
    private List<YoutubeVideoInfo> fetchRecentVideos(String channelId, int maxResults) {
        String url = YT_BASE + "/search?channelId=" + channelId
                + "&part=snippet&order=date&maxResults=" + maxResults
                + "&type=video&key=" + apiKey;

        ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = resp.getBody();
        if (body == null) return List.of();

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null) return List.of();

        List<String> videoIds = items.stream()
                .map(i -> (String) ((Map<String, Object>) i.get("id")).get("videoId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 전체 설명(description) 가져오기
        Map<String, String> descMap = fetchVideoDescriptions(videoIds);

        return items.stream().map(item -> {
            Map<String, Object> id = (Map<String, Object>) item.get("id");
            Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
            Map<String, Object> medThumb = (Map<String, Object>) thumbnails.get("medium");

            String vid = (String) id.get("videoId");
            return YoutubeVideoInfo.builder()
                    .videoId(vid)
                    .title((String) snippet.get("title"))
                    .publishedAt(formatDate((String) snippet.get("publishedAt")))
                    .thumbnailUrl(medThumb != null ? (String) medThumb.get("url") : null)
                    .description(descMap.getOrDefault(vid, ""))
                    .build();
        }).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchVideoDescriptions(List<String> videoIds) {
        if (videoIds.isEmpty()) return Map.of();
        String ids = String.join(",", videoIds);
        String url = YT_BASE + "/videos?id=" + ids + "&part=snippet&key=" + apiKey;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null) return Map.of();
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null) return Map.of();
            Map<String, String> result = new HashMap<>();
            for (Map<String, Object> item : items) {
                String vid = (String) item.get("id");
                Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                StringBuilder text = new StringBuilder();
                text.append(snippet.getOrDefault("description", ""));
                // 태그도 추가
                Object tags = snippet.get("tags");
                if (tags instanceof List) {
                    ((List<?>) tags).forEach(t -> text.append(" ").append(t));
                }
                result.put(vid, text.toString());
            }
            return result;
        } catch (Exception e) {
            log.warn("설명 조회 실패", e);
            return Map.of();
        }
    }

    private Set<String> extractStockCodes(List<YoutubeVideoInfo> videos) {
        Set<String> codes = new LinkedHashSet<>();
        for (YoutubeVideoInfo v : videos) {
            String text = v.getTitle() + " " + v.getDescription();
            for (Map.Entry<String, String> entry : STOCK_ALIAS.entrySet()) {
                if (text.contains(entry.getKey())) {
                    codes.add(entry.getValue());
                }
            }
        }
        return codes;
    }

    // code → name 역방향 맵
    public static final Map<String, String> CODE_TO_NAME = new LinkedHashMap<>();
    static {
        STOCK_ALIAS.forEach((name, code) -> CODE_TO_NAME.putIfAbsent(code, name));
    }

    // 테마 키워드 → 관련 종목 코드 목록
    public static final Map<String, List<String>> THEME_MAP = new LinkedHashMap<>();
    static {
        List<String> ai = List.of("042700","000660","005930","035420","058470","403870","277810","454910");
        THEME_MAP.put("AI",      ai);
        THEME_MAP.put("인공지능", ai);
        THEME_MAP.put("AI반도체", ai);
        THEME_MAP.put("LLM",     ai);

        List<String> semi = List.of("005930","000660","042700","058470","403870","009150","240810","036930");
        THEME_MAP.put("반도체",   semi);
        THEME_MAP.put("HBM",     List.of("000660","042700","005930"));
        THEME_MAP.put("파운드리", List.of("005930","000660"));

        List<String> battery = List.of("373220","006400","051910","247540","086520","003670");
        THEME_MAP.put("2차전지",  battery);
        THEME_MAP.put("배터리",   battery);
        THEME_MAP.put("전기차",   battery);

        List<String> robot = List.of("454910","277810","012450","267260","298040");
        THEME_MAP.put("로봇",    robot);
        THEME_MAP.put("로보틱스", robot);

        THEME_MAP.put("바이오",   List.of("068270","207940","128940","000100","196170","141080"));
        THEME_MAP.put("제약",     List.of("068270","207940","128940","000100"));
        THEME_MAP.put("방산",     List.of("012450","042660","034020","267260"));
        THEME_MAP.put("조선",     List.of("009540","329180","042660"));
        THEME_MAP.put("자동차",   List.of("005380","000270","012330","086280"));
        THEME_MAP.put("금융",     List.of("105560","055550","086790","316140","138040"));
        THEME_MAP.put("양자",     List.of("035420","005930","000660"));
    }

    // 테마 맵에 사용된 코드 → 이름 보완
    static {
        STOCK_ALIAS.put("한미반도체",   "042700");
        STOCK_ALIAS.put("리노공업",     "058470");
        STOCK_ALIAS.put("HPSP",        "403870");
        STOCK_ALIAS.put("원익IPS",      "240810");
        STOCK_ALIAS.put("주성엔지니어링","036930");
        CODE_TO_NAME.put("042700", "한미반도체");
        CODE_TO_NAME.put("058470", "리노공업");
        CODE_TO_NAME.put("403870", "HPSP");
        CODE_TO_NAME.put("240810", "원익IPS");
        CODE_TO_NAME.put("036930", "주성엔지니어링");
    }

    private List<StockMentionResult> fetchPrices(Set<String> codes) {
        List<StockMentionResult> result = new ArrayList<>();
        for (String code : codes) {
            try {
                StockPrice price = stockService.getPrice(code);
                String name = (price.getName() != null && !price.getName().isBlank())
                        ? price.getName()
                        : CODE_TO_NAME.getOrDefault(code, code);
                result.add(StockMentionResult.builder()
                        .code(code)
                        .name(name)
                        .currentPrice(price.getCurrentPrice())
                        .change(price.getChange())
                        .changeRate(price.getChangeRate())
                        .up(price.isUp())
                        .build());
            } catch (Exception e) {
                log.warn("가격 조회 실패: {}", code, e);
            }
        }
        return result;
    }

    private String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return iso;
        return iso.substring(0, 10);
    }

    public void clearCache() {
        cache.clear();
        cacheTime.clear();
    }
}
