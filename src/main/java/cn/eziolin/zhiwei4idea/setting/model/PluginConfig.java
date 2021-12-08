package cn.eziolin.zhiwei4idea.setting.model;

public class PluginConfig {
  private String myDomain;
  private String myUsername;
  private String myPassword;

  public String getUsername() {
    return myUsername;
  }

  public void setUsername(String username) {
    this.myUsername = username;
  }

  public String getPassword() {
    return myPassword;
  }

  public void setPassword(String password) {
    this.myPassword = password;
  }

  public String getDomain() {
    return myDomain;
  }

  public void setDomain(String domain) {
    this.myDomain = domain;
  }
}
