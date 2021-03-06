package person.pluto.natcross2;

import java.io.FileInputStream;
import java.net.ServerSocket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.lang3.StringUtils;

import person.pluto.natcross2.serverside.client.ClientServiceThread;
import person.pluto.natcross2.serverside.client.config.SecretSimpleClientServiceConfig;
import person.pluto.natcross2.serverside.client.config.SimpleClientServiceConfig;
import person.pluto.natcross2.serverside.listen.ListenServerControl;
import person.pluto.natcross2.serverside.listen.config.SimpleListenServerConfig;
import person.pluto.natcross2.serverside.listen.serversocket.ICreateServerSocket;
import person.pluto.natcross2.serverside.listen.config.AllSecretSimpleListenServerConfig;
import person.pluto.natcross2.serverside.listen.config.SecretSimpleListenServerConfig;

/**
 * 
 * <p>
 * 服务端，放在外网侧
 * </p>
 *
 * @author Pluto
 * @since 2020-01-09 16:27:03
 */
public class ServerApp {

    // 服务端地址，支持IP或域名，这个根据服务端放的网络位置进行设置
    public static final String serviceIp = "127.0.0.1";
    // 客户端服务的端口
    public static final int servicePort = 10010;
    // 服务端监听的端口，外网访问服务端IP:listengPort即可进行穿透
    public static final int listenPort = 8081;

    // 交互密钥 AES
    public static final String aesKey = "0PMudFSqJ9WsQrTC60sva9sJAV4PF5iOBjKZW17NeF4=";
    // 交互签名key
    public static final String tokenKey = "tokenKey";

    // 你的p12格式的证书路径
    private static String sslKeyStorePath = System.getenv("sslKeyStorePath");
    // 你的证书密码
    private static String sslKeyStorePassword = System.getenv("sslKeyStorePassword");

    public static ICreateServerSocket createServerSocket;

    public static void main(String[] args) throws Exception {

        // 如果需要HTTPS协议的支持，则填写sslKeyStorePath、sslKeyStorePassword或在环境变量中定义
        if (StringUtils.isNoneBlank(sslKeyStorePath, sslKeyStorePassword)) {
            createServerSocket = new ICreateServerSocket() {
                @Override
                public ServerSocket createServerSocket(int listenPort) throws Exception {
                    KeyStore kstore = KeyStore.getInstance("PKCS12");
                    kstore.load(new FileInputStream(sslKeyStorePath), sslKeyStorePassword.toCharArray());
                    KeyManagerFactory keyFactory = KeyManagerFactory.getInstance("sunx509");
                    keyFactory.init(kstore, sslKeyStorePassword.toCharArray());

                    SSLContext ctx = SSLContext.getInstance("TLSv1.2");
                    ctx.init(keyFactory.getKeyManagers(), null, null);

                    SSLServerSocketFactory serverSocketFactory = ctx.getServerSocketFactory();

                    return serverSocketFactory.createServerSocket(listenPort);
                }
            };
        }

//        simple();
        secret();
//        secretAll();
    }

    /**
     * 交互、隧道都进行加密
     * 
     * @author Pluto
     * @since 2020-01-08 17:29:26
     * @throws Exception
     */
    public static void secretAll() throws Exception {
        // 设置并启动客户端服务线程
        SecretSimpleClientServiceConfig config = new SecretSimpleClientServiceConfig(servicePort);
        // 设置交互aes密钥和签名密钥
        config.setBaseAesKey(aesKey);
        config.setTokenKey(tokenKey);
        new ClientServiceThread(config).start();

        AllSecretSimpleListenServerConfig listengConfig = new AllSecretSimpleListenServerConfig(listenPort);
        // 设置交互aes密钥和签名密钥，这里使用和客户端服务相同的密钥，可以根据需要设置不同的
        listengConfig.setBaseAesKey(aesKey);
        listengConfig.setTokenKey(tokenKey);
        // 设置隧道密钥
        listengConfig.setBasePasswayKey(aesKey);
        listengConfig.setCreateServerSocket(createServerSocket);
        ListenServerControl.createNewListenServer(listengConfig);
    }

    /**
     * 交互加密，即交互验证
     * 
     * @author Pluto
     * @since 2020-01-08 17:28:54
     * @throws Exception
     */
    public static void secret() throws Exception {
        // 设置并启动客户端服务线程
        SecretSimpleClientServiceConfig config = new SecretSimpleClientServiceConfig(servicePort);
        // 设置交互aes密钥和签名密钥
        config.setBaseAesKey(aesKey);
        config.setTokenKey(tokenKey);
        new ClientServiceThread(config).start();

        // 设置并启动一个穿透端口
        SecretSimpleListenServerConfig listengConfig = new SecretSimpleListenServerConfig(listenPort);
        // 设置交互aes密钥和签名密钥，这里使用和客户端服务相同的密钥，可以根据需要设置不同的
        listengConfig.setBaseAesKey(aesKey);
        listengConfig.setTokenKey(tokenKey);
        listengConfig.setCreateServerSocket(createServerSocket);
        ListenServerControl.createNewListenServer(listengConfig);
    }

    /**
     * 无加密、无验证
     * 
     * @author Pluto
     * @since 2020-01-08 17:29:02
     * @throws Exception
     */
    public static void simple() throws Exception {
        // 设置并启动客户端服务线程
        SimpleClientServiceConfig config = new SimpleClientServiceConfig(servicePort);
        new ClientServiceThread(config).start();

        // 设置并启动一个穿透端口
        SimpleListenServerConfig listengConfig = new SimpleListenServerConfig(listenPort);
        listengConfig.setCreateServerSocket(createServerSocket);
        ListenServerControl.createNewListenServer(listengConfig);
    }

}
