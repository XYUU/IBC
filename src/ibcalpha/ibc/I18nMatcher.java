package ibcalpha.ibc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class I18nMatcher {
    private static final Logger logger = LoggerFactory.getLogger(I18nMatcher.class);
    private static final I18nMatcher instance = new I18nMatcher();
    // 核心缓存结构
    private final Map<String, Map<String, String[]>> namespaceCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<String>>> namespaceIndex = new ConcurrentHashMap<>();

    // 私有构造函数，防止外部实例化
    private I18nMatcher() {}

    /**
     * 获取或加载命名空间（核心懒加载逻辑）
     * 使用双重检查锁定，确保多线程并发时，同一个 namespace 只会加载一次
     */
    private Map<String, String[]> getOrLoadNamespace(String namespace) {
        Map<String, String[]> tokenCache = namespaceCache.get(namespace);
        if (tokenCache == null) {
            synchronized (this) {
                // 二次检查
                tokenCache = namespaceCache.get(namespace);
                if (tokenCache == null) {
                    try {
                        // 自动通过原生 ResourceBundle 载入
                        ResourceBundle bundle = ResourceBundle.getBundle(namespace);
                        tokenCache = registerBundle(namespace, bundle);
                    } catch (MissingResourceException e) {
                        logger.error("Failed to load ResourceBundle for namespace: {}", namespace, e);
                        // 放入一个空 Map 防止缓存穿透导致频繁触发异常
                        tokenCache = new ConcurrentHashMap<>();
                        namespaceCache.put(namespace, tokenCache);
                    }
                }
            }
        }
        return tokenCache;
    }

    /**
     * 解析 Bundle 并写入缓存
     */
    private Map<String, String[]> registerBundle(String cacheKey, ResourceBundle bundle) {
        logger.info("Loading and indexing ResourceBundle [{}], language: {}", cacheKey, bundle.getLocale().getLanguage());
        Map<String, String[]> tokenCache = new ConcurrentHashMap<>();
        Map<String, List<String>> firstTokenIndex = new ConcurrentHashMap<>();

        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String template = bundle.getString(key);

            String[] tokens = template.split("%s", -1);
            // 忽略大小写
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].toUpperCase();
            }
            tokenCache.put(key, tokens);

            String firstToken = tokens[0];
            firstTokenIndex.computeIfAbsent(firstToken, k -> new ArrayList<>()).add(key);
        }

        namespaceCache.put(cacheKey, tokenCache);
        namespaceIndex.put(cacheKey, firstTokenIndex);
        return tokenCache;
    }

    /**
     * 判断目标文本是否匹配特定命名空间和 Key 的模板（自动支持 Locale 路由）
     */
    public static boolean isMatchByKey(String namespace, String key, String targetText) {
        Map<String, String[]> tokenCache = instance.getOrLoadNamespace(namespace);
        String[] tokens = tokenCache.get(key);
        return instance.isMatchByTokens(tokens, targetText);
    }

    private boolean isMatchByTokens(String[] tokens, String targetText) {
        if (tokens == null || targetText == null) return false;
        // 忽略大小写
        targetText = targetText.toUpperCase();
        if (tokens.length == 1) return tokens[0].equals(targetText);

        if (!targetText.startsWith(tokens[0])) return false;
        int currentIdx = tokens[0].length();

        for (int i = 1; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if (token.isEmpty()) continue;
            int matchIdx = targetText.indexOf(token, currentIdx);
            if (matchIdx == -1) return false;
            currentIdx = matchIdx + token.length();
        }

        String lastToken = tokens[tokens.length - 1];
        return targetText.endsWith(lastToken) && currentIdx <= targetText.length() - lastToken.length();
    }
}
