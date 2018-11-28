package com.qita.caidan.spider;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.net.ssl.SSLContext;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.DefaultHttpResponseParserFactory;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Company Spider
 * 
 * @author niuyandong
 * @since 2018年11月28日
 */
public class Spider {
    
    private static final String SEARCH_URL = "https://www.tianyancha.com/search?key=";
    
    public static void main(String[] args)
        throws Exception {
        
        HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory() {
            
            @Override
            public HttpMessageParser<HttpResponse> create(SessionInputBuffer buffer, MessageConstraints constraints) {
                LineParser lineParser = new BasicLineParser() {
                    
                    @Override
                    public Header parseHeader(final CharArrayBuffer buffer) {
                        try {
                            return super.parseHeader(buffer);
                        }
                        catch (ParseException ex) {
                            return new BasicHeader(buffer.toString(), null);
                        }
                    }
                    
                };
                return new DefaultHttpResponseParser(buffer, lineParser, DefaultHttpResponseFactory.INSTANCE, constraints);
            }
            
        };
        HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();
        
        // Use a custom connection factory to customize the process of
        // initialization of outgoing HTTP connections. Beside standard connection
        // configuration parameters HTTP connection factory can define message
        // parser / writer routines to be employed by individual connections.
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(requestWriterFactory, responseParserFactory);
        
        // Client HTTP connection objects when fully initialized can be bound to
        // an arbitrary network socket. The process of network socket initialization,
        // its connection to a remote address and binding to a local one is controlled
        // by a connection socket factory.
        
        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        SSLContext sslcontext = SSLContexts.createSystemDefault();
        
        // Create a registry of custom connection socket factories for supported
        // protocol schemes.
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
            RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", new SSLConnectionSocketFactory(sslcontext)).build();
        
        // Use custom DNS resolver to override the system DNS resolution.
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
            
            @Override
            public InetAddress[] resolve(final String host)
                throws UnknownHostException {
                if (host.equalsIgnoreCase("myhost")) {
                    return new InetAddress[]{InetAddress.getByAddress(new byte[]{127, 0, 0, 1})};
                }
                else {
                    return super.resolve(host);
                }
            }
            
        };
        
        // Create a connection manager with custom configuration.
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, connFactory, dnsResolver);
        
        // Create socket configuration
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
        // Configure the connection manager to use socket configuration either
        // by default or for a specific host.
        connManager.setDefaultSocketConfig(socketConfig);
        connManager.setSocketConfig(new HttpHost("www.tianyancha.com", 80), socketConfig);
        // Validate connections after 1 sec of inactivity
        connManager.setValidateAfterInactivity(1000);
        
        // Create message constraints
        MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200).setMaxLineLength(2000).build();
        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setMalformedInputAction(CodingErrorAction.IGNORE)
            .setUnmappableInputAction(CodingErrorAction.IGNORE)
            .setCharset(Consts.UTF_8)
            .setMessageConstraints(messageConstraints)
            .build();
        // Configure the connection manager to use connection configuration either
        // by default or for a specific host.
        connManager.setDefaultConnectionConfig(connectionConfig);
        connManager.setConnectionConfig(new HttpHost("www.tianyancha.com", 80), ConnectionConfig.DEFAULT);
        
        // Configure total max or per route limits for persistent connections
        // that can be kept in the pool or leased by the connection manager.
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(10);
        connManager.setMaxPerRoute(new HttpRoute(new HttpHost("www.tianyancha.com", 80)), 20);
        
        // Use custom cookie store if necessary.
        CookieStore cookieStore = new BasicCookieStore();
        // Use custom credentials provider if necessary.
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.DEFAULT)
            .setExpectContinueEnabled(true)
            .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
            .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
            .build();
        
        // Create an HttpClient with the given custom dependencies and configuration.
        CloseableHttpClient httpclient = HttpClients.custom()
            .setConnectionManager(connManager)
            .setDefaultCookieStore(cookieStore)
            .setDefaultCredentialsProvider(credentialsProvider)
            .setDefaultRequestConfig(defaultRequestConfig)
            .build();
        
        File file = new File("D:\\1.txt");
        Scanner scanner = new Scanner(file);
        
        FileWriter fw = new FileWriter(new File("D:\\2.txt"));
        
        try {
            
            while (scanner.hasNextLine()) {
                
                String name = scanner.nextLine().trim();
                
                HttpGet httpget = new HttpGet("https://www.tianyancha.com/search?key=" + name);
                httpget.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36");
                httpget.addHeader("Accept", "ext/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                httpget.addHeader("Accept-Encoding", "gzip, deflate, br");
                httpget.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
                httpget.addHeader("Cookie",
                    "jsid=SEM-BAIDU-CG-SY-029658; TYCID=0f118570f15911e8ba4f7df869b24c95; undefined=0f118570f15911e8ba4f7df869b24c95; ssuid=9601033536; _ga=GA1.2.1916186720.1543222547; RTYCID=ae5bd0d72937470db2dc23d8406d6d08; CT_TYCID=df48ac55cb08440b844ae45024524cd5; aliyungf_tc=AQAAAKypVRmkQwMAAsZ4agzu3uh8Rfy8; csrfToken=wrlFlMcWXRkF8OC1xrMJRG9h; _gid=GA1.2.537484241.1543375918; cloud_token=a5b409364c804201b139c80baf8a1013; Hm_lvt_e92c8d65d92d534b0fc290df538b4758=1543222547,1543375918,1543376537,1543389616; token=3a2fa35331634d77be980586b8813114; _utm=e0d1b8c7e2cd4a418a985d5f20972cc0; tyc-user-info=%257B%2522myQuestionCount%2522%253A%25220%2522%252C%2522integrity%2522%253A%25220%2525%2522%252C%2522state%2522%253A%25220%2522%252C%2522vipManager%2522%253A%25220%2522%252C%2522onum%2522%253A%25220%2522%252C%2522monitorUnreadCount%2522%253A%25220%2522%252C%2522discussCommendCount%2522%253A%25220%2522%252C%2522token%2522%253A%2522eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMzU1MjI3MDg1MCIsImlhdCI6MTU0MzM4OTY5NCwiZXhwIjoxNTU4OTQxNjk0fQ.seBNS7ETee8mX-Q0yp8idv5a_cknKRBMHF3XNo20oQRpwXyEQUM0GcJ8OWZqjB1ZXPRy0pX1uF3hPCxC6tlVIw%2522%252C%2522redPoint%2522%253A%25220%2522%252C%2522pleaseAnswerCount%2522%253A%25220%2522%252C%2522vnum%2522%253A%25220%2522%252C%2522bizCardUnread%2522%253A%25220%2522%252C%2522mobile%2522%253A%252213552270850%2522%257D; auth_token=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMzU1MjI3MDg1MCIsImlhdCI6MTU0MzM4OTY5NCwiZXhwIjoxNTU4OTQxNjk0fQ.seBNS7ETee8mX-Q0yp8idv5a_cknKRBMHF3XNo20oQRpwXyEQUM0GcJ8OWZqjB1ZXPRy0pX1uF3hPCxC6tlVIw; Hm_lpvt_e92c8d65d92d534b0fc290df538b4758=1543389740");
                
                // Request configuration can be overridden at the request level.
                // They will take precedence over the one set at the client level.
                RequestConfig requestConfig = RequestConfig.copy(defaultRequestConfig).setSocketTimeout(5000).setConnectTimeout(5000).setConnectionRequestTimeout(5000).build();
                httpget.setConfig(requestConfig);
                
                // Execution context can be customized locally.
                HttpClientContext context = HttpClientContext.create();
                // Contextual attributes set the local context level will take
                // precedence over those set at the client level.
                context.setCookieStore(cookieStore);
                context.setCredentialsProvider(credentialsProvider);
                
                System.out.println("executing request " + httpget.getURI());
                CloseableHttpResponse response = httpclient.execute(httpget, context);
                try {
                    System.out.println("----------------------------------------");
                    System.out.println(response.getStatusLine());
                    Document document = Jsoup.parse(EntityUtils.toString(response.getEntity()));
                    Element element = document.selectFirst("div#web-content");
                    Element resultElement = element.selectFirst("div.container")
                        .selectFirst("div.container-left")
                        .selectFirst("div.search-block")
                        .selectFirst("div.result-list")
                        .selectFirst("div.search-item")
                        .selectFirst("div.content");
                    Element urlElement = resultElement.selectFirst("a");
                    String url = urlElement.attr("href");
                    
                    Element stateElement = resultElement.selectFirst("div.header").selectFirst("div.tag");
                    String state = stateElement.html();
                    System.out.println(state);
                    
                    Element humanElement = resultElement.selectFirst("div.info").select("div.title").get(0).selectFirst("a");
                    String human = humanElement.html();
                    System.out.println(human);
                    
                    Element capitalElement = resultElement.selectFirst("div.info").select("div.title").get(1).selectFirst("span");
                    String capital = capitalElement.html();
                    System.out.println(capital);
                    
                    Element regDateElement = resultElement.selectFirst("div.info").select("div.title").get(2).selectFirst("span");
                    String regDate = regDateElement.html();
                    System.out.println(regDate);
                    
                    HttpGet httpGetDetail = new HttpGet(url);
                    httpGetDetail.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.67 Safari/537.36");
                    httpGetDetail.addHeader("Accept", "ext/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                    httpGetDetail.addHeader("Accept-Encoding", "gzip, deflate, br");
                    httpGetDetail.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
                    httpGetDetail.addHeader("Cookie",
                        "jsid=SEM-BAIDU-CG-SY-029658; TYCID=0f118570f15911e8ba4f7df869b24c95; undefined=0f118570f15911e8ba4f7df869b24c95; ssuid=9601033536; _ga=GA1.2.1916186720.1543222547; RTYCID=ae5bd0d72937470db2dc23d8406d6d08; CT_TYCID=df48ac55cb08440b844ae45024524cd5; aliyungf_tc=AQAAAKypVRmkQwMAAsZ4agzu3uh8Rfy8; csrfToken=wrlFlMcWXRkF8OC1xrMJRG9h; _gid=GA1.2.537484241.1543375918; cloud_token=a5b409364c804201b139c80baf8a1013; Hm_lvt_e92c8d65d92d534b0fc290df538b4758=1543222547,1543375918,1543376537,1543389616; token=3a2fa35331634d77be980586b8813114; _utm=e0d1b8c7e2cd4a418a985d5f20972cc0; tyc-user-info=%257B%2522myQuestionCount%2522%253A%25220%2522%252C%2522integrity%2522%253A%25220%2525%2522%252C%2522state%2522%253A%25220%2522%252C%2522vipManager%2522%253A%25220%2522%252C%2522onum%2522%253A%25220%2522%252C%2522monitorUnreadCount%2522%253A%25220%2522%252C%2522discussCommendCount%2522%253A%25220%2522%252C%2522token%2522%253A%2522eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMzU1MjI3MDg1MCIsImlhdCI6MTU0MzM4OTY5NCwiZXhwIjoxNTU4OTQxNjk0fQ.seBNS7ETee8mX-Q0yp8idv5a_cknKRBMHF3XNo20oQRpwXyEQUM0GcJ8OWZqjB1ZXPRy0pX1uF3hPCxC6tlVIw%2522%252C%2522redPoint%2522%253A%25220%2522%252C%2522pleaseAnswerCount%2522%253A%25220%2522%252C%2522vnum%2522%253A%25220%2522%252C%2522bizCardUnread%2522%253A%25220%2522%252C%2522mobile%2522%253A%252213552270850%2522%257D; auth_token=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMzU1MjI3MDg1MCIsImlhdCI6MTU0MzM4OTY5NCwiZXhwIjoxNTU4OTQxNjk0fQ.seBNS7ETee8mX-Q0yp8idv5a_cknKRBMHF3XNo20oQRpwXyEQUM0GcJ8OWZqjB1ZXPRy0pX1uF3hPCxC6tlVIw; Hm_lpvt_e92c8d65d92d534b0fc290df538b4758=1543389740");
                    
                    httpGetDetail.setConfig(requestConfig);
                    
                    System.out.println("executing request " + httpGetDetail.getURI());
                    CloseableHttpResponse detailResp = httpclient.execute(httpGetDetail, context);
                    
                    System.out.println("----------------------------------------");
                    System.out.println(detailResp.getStatusLine());
                    Document detailDocument = Jsoup.parse(EntityUtils.toString(detailResp.getEntity()));
                    
                    Element tbodyElement = detailDocument.selectFirst("div.data-content").select("table.table").get(1).selectFirst("tbody");
                    List<Element> trElements = tbodyElement.select("tr");
                    
                    String companyType = trElements.get(1).select("td").get(3).html();
                    String industry = trElements.get(2).select("td").get(3).html();
                    String djOrg = trElements.get(5).select("td").get(3).html();
                    String addr = trElements.get(7).select("td").get(1).html();
                    addr = addr.substring(0, addr.indexOf("\n"));
                    String optFw = trElements.get(8).select("td").get(1).selectFirst("span.js-full-container").html();
                    
                    StringBuilder sb = new StringBuilder();
                    
                    sb.append(name)
                        .append("    ")
                        .append(human)
                        .append("    ")
                        .append(capital)
                        .append("    ")
                        .append(regDate)
                        .append("    ")
                        .append(state)
                        .append("    ")
                        .append(companyType)
                        .append("    ")
                        .append(industry)
                        .append("    ")
                        .append(djOrg)
                        .append("    ")
                        .append(addr)
                        .append("    ")
                        .append(optFw);
                    
                    fw.write(sb.toString());
                    fw.write("\r\n");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    System.out.println("==> 找不到公司信息：" + name);
                }
                finally {
                    response.close();
                }
                
                Thread.sleep(10000);
            }
            
        }
        finally {
            httpclient.close();
            scanner.close();
            fw.close();
        }
    }
}
