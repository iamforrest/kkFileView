package cn.keking.utils;

import io.mola.galimatias.GalimatiasParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : kl
 * create : 2020-12-27 1:30 上午
 **/
public class WebUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebUtils.class);
    private static final String BASE64_MSG = "base64";

    /**
     * 获取标准的URL
     *
     * @param urlStr url
     * @return 标准的URL
     */
    public static URL normalizedURL(String urlStr) throws GalimatiasParseException, MalformedURLException {
        return io.mola.galimatias.URL.parse(urlStr).toJavaURL();
    }


    /**
     * 对文件名进行编码
     */
    public static String encodeFileName(String name) {
        try {
            name = URLEncoder.encode(name, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return name;
    }


    /**
     * 去除fullfilename参数
     *
     * @param urlStr
     * @return
     */
    public static String clearFullfilenameParam(String urlStr) {
        // 去除特定参数字段
        Pattern pattern = Pattern.compile("(&fullfilename=[^&]*)");
        Matcher matcher = pattern.matcher(urlStr);
        return matcher.replaceAll("");
    }

    /**
     * 对URL进行编码
     */
    public static String urlEncoderencode(String urlStr) {

        String fullFileName = getUrlParameterReg(urlStr, "fullfilename");  //获取流文件名
        if (org.springframework.util.StringUtils.hasText(fullFileName)) {
            // 移除fullfilename参数
            urlStr = clearFullfilenameParam(urlStr);
        } else {
            fullFileName = getFileNameFromURL(urlStr); //获取文件名
        }
        if (KkFileUtils.isIllegalFileName(fullFileName)) { //判断文件名是否带有穿越漏洞
            return null;
        }
        if (!UrlEncoderUtils.hasUrlEncoded(fullFileName)) {  //判断文件名是否转义
            try {
                urlStr = URLEncoder.encode(urlStr, "UTF-8").replaceAll("\\+", "%20").replaceAll("%3A", ":").replaceAll("%2F", "/").replaceAll("%3F", "?").replaceAll("%26", "&").replaceAll("%3D", "=");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return urlStr;
    }

    /**
     * 获取url中的参数
     *
     * @param url  url
     * @param name 参数名
     * @return 参数值
     */
    public static String getUrlParameterReg(String url, String name) {
        Map<String, String> mapRequest = new HashMap<>();
        String strUrlParam = truncateUrlPage(url);
        if (strUrlParam == null) {
            return "";
        }
        //每个键值为一组
        String[] arrSplit = strUrlParam.split("[&]");
        for (String strSplit : arrSplit) {
            String[] arrSplitEqual = strSplit.split("[=]");
            //解析出键值
            if (arrSplitEqual.length > 1) {
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            } else if (!arrSplitEqual[0].equals("")) {
                //只有参数没有值，不加入
                mapRequest.put(arrSplitEqual[0], "");
            }
        }
        return mapRequest.get(name);
    }


    /**
     * 去掉url中的路径，留下请求参数部分
     *
     * @param strURL url地址
     * @return url请求参数部分
     */
    private static String truncateUrlPage(String strURL) {
        String strAllParam = null;
        strURL = strURL.trim();
        String[] arrSplit = strURL.split("[?]");
        if (strURL.length() > 1) {
            if (arrSplit.length > 1) {
                if (arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            }
        }
        return strAllParam;
    }

    /**
     * 从url中剥离出文件名
     *
     * @param url 格式如：http://www.com.cn/20171113164107_月度绩效表模板(新).xls?UCloudPublicKey=ucloudtangshd@weifenf.com14355492830001993909323&Expires=&Signature=I D1NOFtAJSPT16E6imv6JWuq0k=
     * @return 文件名
     */
    public static String getFileNameFromURL(String url) {
        if (url.toLowerCase().startsWith("file:")) {
            try {
                URL urlObj = new URL(url);
                url = urlObj.getPath().substring(1);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        // 因为url的参数中可能会存在/的情况，所以直接url.lastIndexOf("/")会有问题
        // 所以先从？处将url截断，然后运用url.lastIndexOf("/")获取文件名
        String noQueryUrl = url.substring(0, url.contains("?") ? url.indexOf("?") : url.length());
        return noQueryUrl.substring(noQueryUrl.lastIndexOf("/") + 1);
    }

    /**
     * 从url中剥离出文件名
     *
     * @param file 文件
     * @return 文件名
     */
    public static String getFileNameFromMultipartFile(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        //判断是否为IE浏览器的文件名，IE浏览器下文件名会带有盘符信
        // escaping dangerous characters to prevent XSS
        assert fileName != null;
        fileName = HtmlUtils.htmlEscape(fileName, KkFileUtils.DEFAULT_FILE_ENCODING);

        // Check for Unix-style path
        int unixSep = fileName.lastIndexOf('/');
        // Check for Windows-style path
        int winSep = fileName.lastIndexOf('\\');
        // Cut off at latest possible point
        int pos = (Math.max(winSep, unixSep));
        if (pos != -1) {
            fileName = fileName.substring(pos + 1);
        }
        return fileName;
    }


    /**
     * 从url中获取文件后缀
     *
     * @param url url
     * @return 文件后缀
     */
    public static String suffixFromUrl(String url) {
        String nonPramStr = url.substring(0, url.contains("?") ? url.indexOf("?") : url.length());
        String fileName = nonPramStr.substring(nonPramStr.lastIndexOf("/") + 1);
        return KkFileUtils.suffixFromFileName(fileName);
    }

    /**
     * 对url中的文件名进行UTF-8编码
     *
     * @param url url
     * @return 文件名编码后的url
     */
    public static String encodeUrlFileName(String url) {
        String encodedFileName;
        String noQueryUrl = url.substring(0, url.contains("?") ? url.indexOf("?") : url.length());
        int fileNameStartIndex = noQueryUrl.lastIndexOf('/') + 1;
        int fileNameEndIndex = noQueryUrl.lastIndexOf('.');
        if (fileNameEndIndex < fileNameStartIndex) {
            return url;
        }
        try {
            encodedFileName = URLEncoder.encode(noQueryUrl.substring(fileNameStartIndex, fileNameEndIndex), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return url.substring(0, fileNameStartIndex) + encodedFileName + url.substring(fileNameEndIndex);
    }

    /**
     * 从 ServletRequest 获取预览的源 url , 已 base64 解码
     *
     * @param request 请求 request
     * @return url
     */
    public static String getSourceUrl(ServletRequest request) {
        String url = request.getParameter("url");
        String urls = request.getParameter("urls");
        String currentUrl = request.getParameter("currentUrl");
        String urlPath = request.getParameter("urlPath");
        if (StringUtils.isNotBlank(url)) {
            return decodeUrl(url);
        }
        if (StringUtils.isNotBlank(currentUrl)) {
            return decodeUrl(currentUrl);
        }
        if (StringUtils.isNotBlank(urlPath)) {
            return decodeUrl(urlPath);
        }
        if (StringUtils.isNotBlank(urls)) {
            urls = decodeUrl(urls);
            String[] images = urls.split("\\|");
            return images[0];
        }
        return null;
    }

    /**
     * 判断地址是否正确
     * 高 2022/12/17
     */
    public static boolean isValidUrl(String url) {
        String regStr = "^((https|http|ftp|rtsp|mms|file)://)";//[.?*]表示匹配的就是本身
        Pattern pattern = Pattern.compile(regStr);
        Matcher matcher = pattern.matcher(url);
        return matcher.find();
    }

    /**
     * 将 Base64 字符串解码，再解码URL参数, 默认使用 UTF-8
     *
     * @param source 原始 Base64 字符串
     * @return decoded string
     * <p>
     * aHR0cHM6Ly9maWxlLmtla2luZy5jbi9kZW1vL%2BS4reaWhy5wcHR4 -> https://file.keking.cn/demo/%E4%B8%AD%E6%96%87.pptx -> https://file.keking.cn/demo/中文.pptx
     */
    public static String decodeUrl(String source) {
        try {
            return decrypt(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String source) throws Exception {
        String urlDecryptKey = System.getenv("URL_DECRYPT_KEY");
        if (StringUtils.isBlank(urlDecryptKey)) {
            throw new Exception("URL_DECRYPT_KEY not set");
        }
        return decrypt(source, urlDecryptKey);
    }

    public static String encrypt(String source) throws Exception {
        String urlDecryptKey = System.getenv("URL_DECRYPT_KEY");
        if (StringUtils.isBlank(urlDecryptKey)) {
            throw new Exception("URL_DECRYPT_KEY not set");
        }
        return encrypt(source, urlDecryptKey);
    }

    public static String encrypt(String plaintext, String keyString) throws Exception {
        byte[] key = generateKeyFromString(keyString);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decrypt(String ciphertext, String keyString) throws Exception {
        byte[] key = generateKeyFromString(keyString);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }

    private static byte[] generateKeyFromString(String keyString) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(keyString.getBytes("UTF-8"));
    }

    /**
     * 将 Base64 字符串使用指定字符集解码
     *
     * @param source   原始 Base64 字符串
     * @param charsets 字符集
     * @return decoded string
     */
    public static String decodeBase64String(String source, Charset charsets) {
        /*
         * url 传入的参数里加号会被替换成空格，导致解析出错，这里需要把空格替换回加号
         * 有些 Base64 实现可能每 76 个字符插入换行符，也一并去掉
         * https://github.com/kekingcn/kkFileView/pull/340
         */
        try {
            return new String(Base64Utils.decodeFromString(source.replaceAll(" ", "+").replaceAll("\n", "")), charsets);
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains(BASE64_MSG)) {
                LOGGER.error("url解码异常，接入方法错误未使用BASE64");
            } else {
                LOGGER.error("url解码异常，其他错误", e);
            }
            return null;
        }
    }

    /**
     * 获取 url 的 host
     *
     * @param urlStr url
     * @return host
     */
    public static String getHost(String urlStr) {
        try {
            URL url = new URL(urlStr);
            return url.getHost().toLowerCase();
        } catch (MalformedURLException ignored) {
        }
        return null;
    }

    /**
     * 获取 session 中的 String 属性
     *
     * @param request 请求
     * @return 属性值
     */
    public static String getSessionAttr(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 获取 session 中的 long 属性
     *
     * @param request 请求
     * @param key     属性名
     * @return 属性值
     */
    public static long getLongSessionAttr(HttpServletRequest request, String key) {
        String value = getSessionAttr(request, key);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    /**
     * session 中设置属性
     *
     * @param request 请求
     * @param key     属性名
     */
    public static void setSessionAttr(HttpServletRequest request, String key, Object value) {
        HttpSession session = request.getSession();
        if (session == null) {
            return;
        }
        session.setAttribute(key, value);
    }

    /**
     * 移除 session 中的属性
     *
     * @param request 请求
     * @param key     属性名
     */
    public static void removeSessionAttr(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        if (session == null) {
            return;
        }
        session.removeAttribute(key);
    }
}
