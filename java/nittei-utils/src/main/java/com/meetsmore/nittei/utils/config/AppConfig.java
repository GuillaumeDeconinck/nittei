package com.meetsmore.nittei.utils.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nittei")
public class AppConfig {

  private String tokioRuntimeFlavor = "multi_thread";
  private Integer tokioRuntimeNumberOfWorkers;
  private boolean printRuntimeInfo = false;
  private String httpHost = "127.0.0.1";
  private int httpPort = 5000;
  private long serverShutdownSleep = 5;
  private long serverShutdownTimeout = 10;
  private PgConfig pg = new PgConfig();
  private String createAccountSecretCode;
  private boolean disableReminders = false;
  private int maxEventsReturnedBySearch = 5000;
  private long eventInstancesQueryDurationLimit = 300L * 24 * 60 * 60 * 1000;
  private long bookingSlotsQueryDurationLimit = 100L * 24 * 60 * 60 * 1000;
  private AccountConfig account;
  private ObservabilityConfig observability = new ObservabilityConfig();

  public String getTokioRuntimeFlavor() {
    return tokioRuntimeFlavor;
  }

  public void setTokioRuntimeFlavor(String tokioRuntimeFlavor) {
    this.tokioRuntimeFlavor = tokioRuntimeFlavor;
  }

  public Integer getTokioRuntimeNumberOfWorkers() {
    return tokioRuntimeNumberOfWorkers;
  }

  public void setTokioRuntimeNumberOfWorkers(Integer tokioRuntimeNumberOfWorkers) {
    this.tokioRuntimeNumberOfWorkers = tokioRuntimeNumberOfWorkers;
  }

  public boolean isPrintRuntimeInfo() {
    return printRuntimeInfo;
  }

  public void setPrintRuntimeInfo(boolean printRuntimeInfo) {
    this.printRuntimeInfo = printRuntimeInfo;
  }

  public String getHttpHost() {
    return httpHost;
  }

  public void setHttpHost(String httpHost) {
    this.httpHost = httpHost;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public void setHttpPort(int httpPort) {
    this.httpPort = httpPort;
  }

  public long getServerShutdownSleep() {
    return serverShutdownSleep;
  }

  public void setServerShutdownSleep(long serverShutdownSleep) {
    this.serverShutdownSleep = serverShutdownSleep;
  }

  public long getServerShutdownTimeout() {
    return serverShutdownTimeout;
  }

  public void setServerShutdownTimeout(long serverShutdownTimeout) {
    this.serverShutdownTimeout = serverShutdownTimeout;
  }

  public PgConfig getPg() {
    return pg;
  }

  public void setPg(PgConfig pg) {
    this.pg = pg;
  }

  public String getCreateAccountSecretCode() {
    return createAccountSecretCode;
  }

  public void setCreateAccountSecretCode(String createAccountSecretCode) {
    this.createAccountSecretCode = createAccountSecretCode;
  }

  public boolean isDisableReminders() {
    return disableReminders;
  }

  public void setDisableReminders(boolean disableReminders) {
    this.disableReminders = disableReminders;
  }

  public int getMaxEventsReturnedBySearch() {
    return maxEventsReturnedBySearch;
  }

  public void setMaxEventsReturnedBySearch(int maxEventsReturnedBySearch) {
    this.maxEventsReturnedBySearch = maxEventsReturnedBySearch;
  }

  public long getEventInstancesQueryDurationLimit() {
    return eventInstancesQueryDurationLimit;
  }

  public void setEventInstancesQueryDurationLimit(long eventInstancesQueryDurationLimit) {
    this.eventInstancesQueryDurationLimit = eventInstancesQueryDurationLimit;
  }

  public long getBookingSlotsQueryDurationLimit() {
    return bookingSlotsQueryDurationLimit;
  }

  public void setBookingSlotsQueryDurationLimit(long bookingSlotsQueryDurationLimit) {
    this.bookingSlotsQueryDurationLimit = bookingSlotsQueryDurationLimit;
  }

  public AccountConfig getAccount() {
    return account;
  }

  public void setAccount(AccountConfig account) {
    this.account = account;
  }

  public ObservabilityConfig getObservability() {
    return observability;
  }

  public void setObservability(ObservabilityConfig observability) {
    this.observability = observability;
  }

  public static class ObservabilityConfig {
    private String serviceName = "unknown service";
    private String serviceVersion = "unknown version";
    private String serviceEnv = "unknown env";
    private boolean disableTracing = false;
    private double tracingSampleRate = 0.1;
    private String otlpTracingEndpoint;
    private String datadogTracingEndpoint;
    private boolean observeStatusEndpoints = false;

    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public String getServiceVersion() {
      return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
    }

    public String getServiceEnv() {
      return serviceEnv;
    }

    public void setServiceEnv(String serviceEnv) {
      this.serviceEnv = serviceEnv;
    }

    public boolean isDisableTracing() {
      return disableTracing;
    }

    public void setDisableTracing(boolean disableTracing) {
      this.disableTracing = disableTracing;
    }

    public double getTracingSampleRate() {
      return tracingSampleRate;
    }

    public void setTracingSampleRate(double tracingSampleRate) {
      this.tracingSampleRate = tracingSampleRate;
    }

    public String getOtlpTracingEndpoint() {
      return otlpTracingEndpoint;
    }

    public void setOtlpTracingEndpoint(String otlpTracingEndpoint) {
      this.otlpTracingEndpoint = otlpTracingEndpoint;
    }

    public String getDatadogTracingEndpoint() {
      return datadogTracingEndpoint;
    }

    public void setDatadogTracingEndpoint(String datadogTracingEndpoint) {
      this.datadogTracingEndpoint = datadogTracingEndpoint;
    }

    public boolean isObserveStatusEndpoints() {
      return observeStatusEndpoints;
    }

    public void setObserveStatusEndpoints(boolean observeStatusEndpoints) {
      this.observeStatusEndpoints = observeStatusEndpoints;
    }
  }

  public static class AccountConfig {
    private String secretKey;
    private String id;
    private String webhookUrl;
    private String pubKey;
    private IntegrationConfig google;
    private IntegrationConfig outlook;

    public String getSecretKey() {
      return secretKey;
    }

    public void setSecretKey(String secretKey) {
      this.secretKey = secretKey;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getWebhookUrl() {
      return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
      this.webhookUrl = webhookUrl;
    }

    public String getPubKey() {
      return pubKey;
    }

    public void setPubKey(String pubKey) {
      this.pubKey = pubKey;
    }

    public IntegrationConfig getGoogle() {
      return google;
    }

    public void setGoogle(IntegrationConfig google) {
      this.google = google;
    }

    public IntegrationConfig getOutlook() {
      return outlook;
    }

    public void setOutlook(IntegrationConfig outlook) {
      this.outlook = outlook;
    }
  }

  public static class IntegrationConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
      return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
      this.redirectUri = redirectUri;
    }
  }

  public static class PgConfig {
    private String databaseUrl = "postgresql://postgres:postgres@localhost:45432/nittei";
    private boolean skipMigrations = false;
    private int minConnections = 2;
    private int maxConnections = 5;

    public String getDatabaseUrl() {
      return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
      this.databaseUrl = databaseUrl;
    }

    public boolean isSkipMigrations() {
      return skipMigrations;
    }

    public void setSkipMigrations(boolean skipMigrations) {
      this.skipMigrations = skipMigrations;
    }

    public int getMinConnections() {
      return minConnections;
    }

    public void setMinConnections(int minConnections) {
      this.minConnections = minConnections;
    }

    public int getMaxConnections() {
      return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }
  }
}
