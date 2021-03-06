package org.sterl.gcm._example.server.config;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.util.XmppStringUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.util.StringUtils;

@Configuration
@ImportResource("classpath:gcm-xmpp-beans.xml")
public class GcmConfig {

    @Bean
    public XmppConnectionFactoryBean gcmConnection() {
        // using prod as pre-prod sometimes just didn't work
        // https://developers.google.com/cloud-messaging/ccs#connecting
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setHost("gcm-xmpp.googleapis.com")
                .setPort(5235)
                .setDebuggerEnabled(true)
                .setSecurityMode(SecurityMode.ifpossible)
                .setSendPresence(false)
                .setCompressionEnabled(true)
                .setSocketFactory(SSLSocketFactory.getDefault())
                .setServiceName("gcm-sample")
                .setConnectTimeout((int)TimeUnit.SECONDS.toMillis(10))
                .setUsernameAndPassword("234377203703@gcm.googleapis.com", "AIzaSyCySnZ5Ny9jAXtI7Y17co5fv5PSAG9i5-A").build();
        
        XmppConnectionFactoryBean connectionFactoryBean = new XmppConnectionFactoryBean();
        connectionFactoryBean.setConnectionConfiguration(config);
        

        return connectionFactoryBean;
    }
    
    /**
     * To remove the Roster we have to fix the Spring Xmpp Connection factory as we want to get rid of the dependency -- which otherwise
     * results in a no class def found.
     * 
     * <pre>
     * {@code
     * org.jivesoftware.smack.roster.Roster     : Exception reloading roster
     * org.jivesoftware.smack.SmackException$NoResponseException: No response received within reply timeout. Timeout was 5000ms (~5s). Used filter: IQReplyFilter: iqAndIdFilter (AndFilter: (OrFilter: (IQTypeFilter: type=error, IQTypeFilter: type=result), StanzaIdFilter: id=qNsJ6-8)), : fromFilter (OrFilter: (FromMatchesFilter (full): null, FromMatchesFilter (bare): 234377203703@gcm.googleapis.com, FromMatchesFilter (full): gcm.googleapis.com)).
     * at org.jivesoftware.smack.SmackException$NoResponseException.newWith(SmackException.java:106) ~[smack-core-4.1.6.jar:4.1.6]
     * at org.jivesoftware.smack.AbstractXMPPConnection$6.run(AbstractXMPPConnection.java:1447) [smack-core-4.1.6.jar:4.1.6]
     * at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511) [na:1.8.0_51]
     * at java.util.concurrent.FutureTask.run(FutureTask.java:266) [na:1.8.0_51]
     * at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180) [na:1.8.0_51]
     * at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293) [na:1.8.0_51]
     * at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142) [na:1.8.0_51]
     * at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617) [na:1.8.0_51]
     * at java.lang.Thread.run(Thread.java:745) [na:1.8.0_51]
     * }
     * </pre>
     */
    public class XmppConnectionFactoryBean extends AbstractFactoryBean<XMPPConnection> implements SmartLifecycle {

        private final Object lifecycleMonitor = new Object();

        private XMPPTCPConnectionConfiguration connectionConfiguration;

        private volatile String resource; // server will generate resource if not provided

        private volatile String user;

        private volatile String password;

        private volatile String serviceName;

        private volatile String host;

        private volatile int port = 5222;

        private volatile boolean autoStartup = true;

        private volatile int phase = Integer.MIN_VALUE;

        private volatile boolean running;

        private volatile XMPPTCPConnection connection;


        public XmppConnectionFactoryBean() {
        }

        /**
         * @param connectionConfiguration the {@link XMPPTCPConnectionConfiguration} to use.
         * @deprecated since {@literal 4.2.5} in favor of {@link #setConnectionConfiguration(XMPPTCPConnectionConfiguration)}
         * to avoid {@code BeanCurrentlyInCreationException}
         * during {@code AbstractAutowireCapableBeanFactory.getSingletonFactoryBeanForTypeCheck()}
         */
        @Deprecated
        public XmppConnectionFactoryBean(XMPPTCPConnectionConfiguration connectionConfiguration) {
            this.connectionConfiguration = connectionConfiguration;
        }

        /**
         * @param connectionConfiguration the {@link XMPPTCPConnectionConfiguration} to use.
         * @since 4.2.5
         */
        public void setConnectionConfiguration(XMPPTCPConnectionConfiguration connectionConfiguration) {
            this.connectionConfiguration = connectionConfiguration;
        }

        public void setAutoStartup(boolean autoStartup) {
            this.autoStartup = autoStartup;
        }

        public void setPhase(int phase) {
            this.phase = phase;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public Class<? extends XMPPConnection> getObjectType() {
            return XMPPConnection.class;
        }

        @Override
        protected XMPPConnection createInstance() throws Exception {
            XMPPTCPConnectionConfiguration connectionConfiguration = this.connectionConfiguration;
            if (this.connectionConfiguration == null) {
                XMPPTCPConnectionConfiguration.Builder builder =
                        XMPPTCPConnectionConfiguration.builder()
                                .setHost(this.host)
                                .setPort(this.port)
                                .setResource(this.resource)
                                .setUsernameAndPassword(this.user, this.password)
                                .setServiceName(this.serviceName);

                if (!StringUtils.hasText(this.serviceName) && StringUtils.hasText(this.user)) {
                    builder.setUsernameAndPassword(XmppStringUtils.parseLocalpart(this.user), this.password)
                            .setServiceName(XmppStringUtils.parseDomain(this.user));
                }

                connectionConfiguration = builder.build();
            }
            this.connection = new XMPPTCPConnection(connectionConfiguration);
            return this.connection;
        }

        @Override
        public void start() {
            synchronized (this.lifecycleMonitor) {
                if (this.running) {
                    return;
                }
                try {
                    this.connection.connect();
                    this.connection.addConnectionListener(new LoggingConnectionListener());
                    this.connection.login();
                    this.running = true;
                }
                catch (Exception e) {
                    throw new BeanInitializationException("failed to connect to XMPP service for "
                            + this.connection.getServiceName(), e);
                }
            }
        }

        @Override
        public void stop() {
            synchronized (this.lifecycleMonitor) {
                if (this.isRunning()) {
                    this.connection.disconnect();
                    this.running = false;
                }
            }
        }

        @Override
        public void stop(Runnable callback) {
            stop();
            callback.run();
        }

        @Override
        public boolean isRunning() {
            return this.running;
        }

        @Override
        public int getPhase() {
            return this.phase;
        }

        @Override
        public boolean isAutoStartup() {
            return this.autoStartup;
        }


        private class LoggingConnectionListener implements ConnectionListener {

            @Override
            public void reconnectionSuccessful() {
                logger.debug("Reconnection successful");
            }

            @Override
            public void reconnectionFailed(Exception e) {
                logger.debug("Reconnection failed", e);
            }

            @Override
            public void reconnectingIn(int seconds) {
                logger.debug("Reconnecting in " + seconds + " seconds");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                logger.debug("Connection closed on error", e);
            }

            @Override
            public void connectionClosed() {
                logger.debug("Connection closed");
            }

            @Override
            public void connected(XMPPConnection connection) {
                logger.debug("Connection connected");
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                logger.debug("Connection authenticated");
            }

        }

    }
}
